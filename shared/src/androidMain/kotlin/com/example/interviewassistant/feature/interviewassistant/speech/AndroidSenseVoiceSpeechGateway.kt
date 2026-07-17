package com.example.interviewassistant.feature.interviewassistant.speech

import com.example.interviewassistant.core.audio.AUDIO_SAMPLE_RATE
import com.example.interviewassistant.core.audio.AudioFrame
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
import com.example.interviewassistant.feature.interviewassistant.data.repository.OnDeviceSpeechGateway
import com.example.interviewassistant.feature.interviewassistant.domain.model.SenseVoiceConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechUnderstandingMetadata
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android sherpa-onnx implementation of VAD-assisted simulated-streaming SenseVoice recognition.
 */
class AndroidSenseVoiceSpeechGateway(
    private val modelManager: AndroidSenseVoiceModelManager,
    private val dispatchers: CoroutineDispatcherProvider,
) : OnDeviceSpeechGateway {
    private val runtimeMutex = Mutex()
    private val sessionMutex = Mutex()
    private val stopRequested = AtomicBoolean(false)
    private var runtime: Runtime? = null

    override fun recognize(
        audio: Flow<AudioFrame>,
        configuration: SenseVoiceConfiguration,
    ): Flow<SpeechRecognitionEvent> = flow {
        sessionMutex.withLock {
            stopRequested.set(false)
            val activeRuntime = runtime(configuration)
            val vad = activeRuntime.vad
            vad.reset()
            try {
                val audioBuffer = FloatAudioBuffer()
                var processedSamples = 0
                var speechStarted = false
                var speechStartSample = 0
                var lastPartialAtMillis = 0L
                var lastPartialText = ""
                var lastPartialMetadata = SpeechUnderstandingMetadata()

                audio.takeWhile { !stopRequested.get() }.collect { frame ->
                    audioBuffer.append(frame.pcm.toNormalizedFloatSamples())
                    while (processedSamples + VAD_WINDOW_SAMPLES <= audioBuffer.size) {
                        vad.acceptWaveform(
                            audioBuffer.copyOfRange(
                                processedSamples,
                                processedSamples + VAD_WINDOW_SAMPLES,
                            ),
                        )
                        processedSamples += VAD_WINDOW_SAMPLES
                        if (!speechStarted && vad.isSpeechDetected()) {
                            speechStarted = true
                            speechStartSample = (processedSamples - PRE_ROLL_SAMPLES).coerceAtLeast(0)
                            lastPartialAtMillis = frame.capturedAtMillis
                        }
                    }

                    val shouldDecodePartial = speechStarted &&
                        processedSamples - speechStartSample >= MINIMUM_PARTIAL_SAMPLES &&
                        frame.capturedAtMillis - lastPartialAtMillis >= configuration.partialIntervalMillis
                    if (shouldDecodePartial) {
                        val result = decode(
                            activeRuntime.recognizer,
                            audioBuffer.copyOfRange(speechStartSample, processedSamples),
                        )
                        lastPartialAtMillis = frame.capturedAtMillis
                        if (
                            result.hasContent &&
                            (result.text != lastPartialText || result.metadata != lastPartialMetadata)
                        ) {
                            lastPartialText = result.text
                            lastPartialMetadata = result.metadata
                            emit(SpeechRecognitionEvent.Partial(result.text, result.metadata))
                        }
                    }

                    while (!vad.empty()) {
                        val finalResult = decode(activeRuntime.recognizer, vad.front().samples)
                        vad.pop()
                        if (finalResult.hasContent) {
                            emit(SpeechRecognitionEvent.Final(finalResult.text, finalResult.metadata))
                        }
                        audioBuffer.clear()
                        processedSamples = 0
                        speechStarted = false
                        speechStartSample = 0
                        lastPartialAtMillis = 0L
                        lastPartialText = ""
                        lastPartialMetadata = SpeechUnderstandingMetadata()
                    }
                }

                if (!stopRequested.get()) {
                    vad.flush()
                    while (!vad.empty()) {
                        val finalResult = decode(activeRuntime.recognizer, vad.front().samples)
                        vad.pop()
                        if (finalResult.hasContent) {
                            emit(SpeechRecognitionEvent.Final(finalResult.text, finalResult.metadata))
                        }
                    }
                }
            } finally {
                vad.reset()
            }
        }
    }.flowOn(dispatchers.default)

    override suspend fun stop() {
        stopRequested.set(true)
    }

    private suspend fun runtime(configuration: SenseVoiceConfiguration): Runtime {
        return runtimeMutex.withLock {
            val key = RuntimeKey(
                language = configuration.language,
                useInverseTextNormalization = configuration.useInverseTextNormalization,
                maxSpeechDurationSeconds = configuration.maxSpeechDurationSeconds,
            )
            runtime?.takeIf { it.key == key } ?: createRuntime(key).also { created ->
                runtime?.release()
                runtime = created
            }
        }
    }

    private fun createRuntime(key: RuntimeKey): Runtime {
        val files = checkNotNull(modelManager.modelFilesOrNull()) {
            "SenseVoice model files are not ready"
        }
        val recognizer = OfflineRecognizer(
            assetManager = null,
            config = OfflineRecognizerConfig(
                modelConfig = OfflineModelConfig(
                    senseVoice = OfflineSenseVoiceModelConfig(
                        model = files.model,
                        language = key.language,
                        useInverseTextNormalization = key.useInverseTextNormalization,
                    ),
                    tokens = files.tokens,
                    numThreads = RECOGNIZER_THREADS,
                    provider = "cpu",
                ),
            ),
        )
        val vad = Vad(
            assetManager = null,
            config = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = files.vad,
                    threshold = VAD_THRESHOLD,
                    minSilenceDuration = MIN_SILENCE_SECONDS,
                    minSpeechDuration = MIN_SPEECH_SECONDS,
                    windowSize = VAD_WINDOW_SAMPLES,
                    maxSpeechDuration = key.maxSpeechDurationSeconds.toFloat(),
                ),
                sampleRate = AUDIO_SAMPLE_RATE,
                numThreads = VAD_THREADS,
                provider = "cpu",
            ),
        )
        return Runtime(key, recognizer, vad)
    }

    private fun decode(recognizer: OfflineRecognizer, samples: FloatArray): DecodedSpeech {
        val stream = recognizer.createStream()
        return try {
            stream.acceptWaveform(samples, AUDIO_SAMPLE_RATE)
            recognizer.decode(stream)
            val result = recognizer.getResult(stream)
            DecodedSpeech(
                text = result.text.trim(),
                metadata = SpeechUnderstandingMetadata(
                    language = result.lang.normalizedLabel(),
                    emotion = result.emotion.normalizedLabel(),
                    audioEvent = result.event.normalizedLabel(),
                ),
            )
        } finally {
            stream.release()
        }
    }

    private fun String.normalizedLabel(): String? {
        return trim()
            .removeSurrounding(prefix = "<|", suffix = "|>")
            .trim()
            .takeIf(String::isNotEmpty)
    }

    private data class DecodedSpeech(
        val text: String,
        val metadata: SpeechUnderstandingMetadata,
    ) {
        val hasContent: Boolean
            get() = text.isNotEmpty() || !metadata.isEmpty
    }

    private data class RuntimeKey(
        val language: String,
        val useInverseTextNormalization: Boolean,
        val maxSpeechDurationSeconds: Int,
    )

    private data class Runtime(
        val key: RuntimeKey,
        val recognizer: OfflineRecognizer,
        val vad: Vad,
    ) {
        fun release() {
            vad.release()
            recognizer.release()
        }
    }

    private class FloatAudioBuffer(initialCapacity: Int = AUDIO_SAMPLE_RATE * 4) {
        private var samples = FloatArray(initialCapacity)
        var size: Int = 0
            private set

        fun append(values: FloatArray) {
            ensureCapacity(size + values.size)
            values.copyInto(samples, destinationOffset = size)
            size += values.size
        }

        fun copyOfRange(startIndex: Int, endIndex: Int): FloatArray =
            samples.copyOfRange(startIndex, endIndex)

        fun clear() {
            size = 0
        }

        private fun ensureCapacity(requiredCapacity: Int) {
            if (requiredCapacity <= samples.size) return
            var newCapacity = samples.size
            while (newCapacity < requiredCapacity) newCapacity *= 2
            samples = samples.copyOf(newCapacity)
        }
    }

    private fun ByteArray.toNormalizedFloatSamples(): FloatArray {
        val output = FloatArray(size / BYTES_PER_SAMPLE)
        var sourceIndex = 0
        output.indices.forEach { outputIndex ->
            val low = this[sourceIndex].toInt() and 0xff
            val high = this[sourceIndex + 1].toInt()
            val pcm = (high shl 8) or low
            output[outputIndex] = pcm.toShort() / PCM_NORMALIZATION
            sourceIndex += BYTES_PER_SAMPLE
        }
        return output
    }

    private companion object {
        const val BYTES_PER_SAMPLE = 2
        const val PCM_NORMALIZATION = 32_768f
        const val VAD_WINDOW_SAMPLES = 512
        const val PRE_ROLL_SAMPLES = 6_400
        const val MINIMUM_PARTIAL_SAMPLES = 8_000
        const val VAD_THRESHOLD = 0.5f
        const val MIN_SILENCE_SECONDS = 0.65f
        const val MIN_SPEECH_SECONDS = 0.25f
        const val RECOGNIZER_THREADS = 2
        const val VAD_THREADS = 1
    }
}
