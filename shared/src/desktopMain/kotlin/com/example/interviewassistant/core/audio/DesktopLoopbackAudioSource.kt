package com.example.interviewassistant.core.audio

import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.core.util.TimeProvider
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.COM.COMUtils
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.Guid.CLSID
import com.sun.jna.platform.win32.Guid.GUID
import com.sun.jna.platform.win32.Guid.IID
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.WTypes
import com.sun.jna.platform.win32.WinNT.HRESULT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Windows system-output capture backed directly by WASAPI loopback through JNA.
 */
class DesktopLoopbackAudioSource(
    private val dispatchers: CoroutineDispatcherProvider,
) : AudioSource {
    @Volatile
    private var activeCapture: WasapiLoopbackCapture? = null

    override val isAvailable: Boolean
        get() = Platform.isWindows()

    override fun frames(): Flow<AudioFrame> = callbackFlow {
        if (!Platform.isWindows()) {
            close(AudioCaptureException("WASAPI loopback is available on Windows only"))
            return@callbackFlow
        }
        val capture = WasapiLoopbackCapture()
        activeCapture = capture
        val job = launch(dispatchers.io) {
            try {
                capture.run { frame ->
                    trySend(AudioFrame(frame, TimeProvider.currentTimeMillis())).isSuccess
                }
                close()
            } catch (error: Throwable) {
                close(AudioCaptureException("Windows loopback capture failed", error))
            } finally {
                if (activeCapture === capture) activeCapture = null
            }
        }
        awaitClose {
            capture.stop()
            job.cancel()
        }
    }

    override suspend fun stop() {
        activeCapture?.stop()
    }
}

private class WasapiLoopbackCapture {
    @Volatile
    private var running = true

    fun stop() {
        running = false
    }

    fun run(onFrame: (ByteArray) -> Boolean) {
        val initializeResult = Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, COINIT_MULTITHREADED)
        checkResult(initializeResult, allowFalse = true, operation = "CoInitializeEx")

        var enumerator: MmDeviceEnumerator? = null
        var device: MmDevice? = null
        var audioClient: AudioClient? = null
        var captureClient: AudioCaptureClient? = null
        var formatPointer: Pointer? = null
        try {
            enumerator = MmDeviceEnumerator.create()
            device = enumerator.defaultRenderDevice()
            audioClient = device.activateAudioClient()
            formatPointer = audioClient.mixFormat()
            val format = WaveFormat.read(formatPointer)
            audioClient.initializeLoopback(formatPointer)
            captureClient = audioClient.captureClient()
            val converter = PcmFrameConverter(format)

            checkResult(audioClient.start(), operation = "IAudioClient.Start")
            while (running) {
                var packetFrames = captureClient.nextPacketSize()
                if (packetFrames == 0) {
                    Thread.sleep(POLL_INTERVAL_MILLIS)
                    continue
                }
                while (packetFrames > 0 && running) {
                    val packet = captureClient.buffer(packetFrames, format.blockAlign)
                    try {
                        converter.accept(packet.bytes) { frame ->
                            if (!onFrame(frame)) running = false
                        }
                    } finally {
                        captureClient.releaseBuffer(packet.frames)
                    }
                    packetFrames = captureClient.nextPacketSize()
                }
            }
        } finally {
            runCatching { audioClient?.stopClient() }
            captureClient?.Release()
            audioClient?.Release()
            device?.Release()
            enumerator?.Release()
            formatPointer?.let(Ole32.INSTANCE::CoTaskMemFree)
            Ole32.INSTANCE.CoUninitialize()
        }
    }

    private companion object {
        const val COINIT_MULTITHREADED = 0
        const val POLL_INTERVAL_MILLIS = 5L
    }
}

private open class ComObject(pointer: Pointer) : Unknown(pointer) {
    fun invokeHResult(index: Int, vararg arguments: Any?): HRESULT {
        val callArguments = arrayOfNulls<Any>(arguments.size + 1)
        callArguments[0] = pointer
        arguments.copyInto(callArguments, destinationOffset = 1)
        return _invokeNativeObject(index, callArguments, HRESULT::class.java) as HRESULT
    }
}

private class MmDeviceEnumerator(pointer: Pointer) : ComObject(pointer) {
    fun defaultRenderDevice(): MmDevice {
        val reference = PointerByReference()
        val result = invokeHResult(
            index = 4,
            E_RENDER,
            E_CONSOLE,
            reference,
        )
        checkResult(result, operation = "IMMDeviceEnumerator.GetDefaultAudioEndpoint")
        return MmDevice(reference.value)
    }

    companion object {
        private val CLASS_ID = CLSID("BCDE0395-E52F-467C-8E3D-C4579291692E")
        private val INTERFACE_ID = IID("A95664D2-9614-4F35-A746-DE8DB63617E6")
        private const val E_RENDER = 0
        private const val E_CONSOLE = 0

        fun create(): MmDeviceEnumerator {
            val reference = PointerByReference()
            val result = Ole32.INSTANCE.CoCreateInstance(
                CLASS_ID,
                null,
                WTypes.CLSCTX_ALL,
                INTERFACE_ID,
                reference,
            )
            checkResult(result, operation = "CoCreateInstance(MMDeviceEnumerator)")
            return MmDeviceEnumerator(reference.value)
        }
    }
}

private class MmDevice(pointer: Pointer) : ComObject(pointer) {
    fun activateAudioClient(): AudioClient {
        val reference = PointerByReference()
        val result = invokeHResult(
            index = 3,
            AUDIO_CLIENT_ID,
            WTypes.CLSCTX_ALL,
            Pointer.NULL,
            reference,
        )
        checkResult(result, operation = "IMMDevice.Activate(IAudioClient)")
        return AudioClient(reference.value)
    }

    private companion object {
        val AUDIO_CLIENT_ID = IID("1CB9AD4C-DBFA-4C32-B178-C2F568A703B2")
    }
}

private class AudioClient(pointer: Pointer) : ComObject(pointer) {
    fun mixFormat(): Pointer {
        val reference = PointerByReference()
        checkResult(
            invokeHResult(index = 8, reference),
            operation = "IAudioClient.GetMixFormat",
        )
        return reference.value
    }

    fun initializeLoopback(format: Pointer) {
        checkResult(
            invokeHResult(
                index = 3,
                AUDCLNT_SHAREMODE_SHARED,
                AUDCLNT_STREAMFLAGS_LOOPBACK,
                0L,
                0L,
                format,
                Pointer.NULL,
            ),
            operation = "IAudioClient.Initialize(loopback)",
        )
    }

    fun captureClient(): AudioCaptureClient {
        val reference = PointerByReference()
        checkResult(
            invokeHResult(index = 14, CAPTURE_CLIENT_ID, reference),
            operation = "IAudioClient.GetService(IAudioCaptureClient)",
        )
        return AudioCaptureClient(reference.value)
    }

    fun start(): HRESULT = invokeHResult(index = 10)

    fun stopClient(): HRESULT = invokeHResult(index = 11)

    private companion object {
        const val AUDCLNT_SHAREMODE_SHARED = 0
        const val AUDCLNT_STREAMFLAGS_LOOPBACK = 0x00020000
        val CAPTURE_CLIENT_ID = IID("C8ADBD64-E71E-48A0-A4DE-185C395CD317")
    }
}

private class AudioCaptureClient(pointer: Pointer) : ComObject(pointer) {
    fun nextPacketSize(): Int {
        val frames = IntByReference()
        checkResult(
            invokeHResult(index = 5, frames),
            operation = "IAudioCaptureClient.GetNextPacketSize",
        )
        return frames.value
    }

    fun buffer(expectedFrames: Int, blockAlign: Int): CapturePacket {
        val data = PointerByReference()
        val frames = IntByReference()
        val flags = IntByReference()
        val devicePosition = LongByReference()
        val qpcPosition = LongByReference()
        checkResult(
            invokeHResult(
                index = 3,
                data,
                frames,
                flags,
                devicePosition,
                qpcPosition,
            ),
            operation = "IAudioCaptureClient.GetBuffer",
        )
        val frameCount = frames.value.takeIf { it > 0 } ?: expectedFrames
        val byteCount = frameCount * blockAlign
        val bytes = if (flags.value and AUDCLNT_BUFFERFLAGS_SILENT != 0 || data.value == Pointer.NULL) {
            ByteArray(byteCount)
        } else {
            data.value.getByteArray(0, byteCount)
        }
        return CapturePacket(frameCount, bytes)
    }

    fun releaseBuffer(frames: Int) {
        checkResult(
            invokeHResult(index = 4, frames),
            operation = "IAudioCaptureClient.ReleaseBuffer",
        )
    }

    private companion object {
        const val AUDCLNT_BUFFERFLAGS_SILENT = 0x2
    }
}

private data class CapturePacket(
    val frames: Int,
    val bytes: ByteArray,
)

private data class WaveFormat(
    val formatTag: Int,
    val channels: Int,
    val sampleRate: Int,
    val blockAlign: Int,
    val bitsPerSample: Int,
    val isFloat: Boolean,
) {
    companion object {
        private const val WAVE_FORMAT_PCM = 1
        private const val WAVE_FORMAT_IEEE_FLOAT = 3
        private const val WAVE_FORMAT_EXTENSIBLE = 0xFFFE

        fun read(pointer: Pointer): WaveFormat {
            val tag = pointer.getShort(0).toInt() and 0xFFFF
            val channels = pointer.getShort(2).toInt() and 0xFFFF
            val sampleRate = pointer.getInt(4)
            val blockAlign = pointer.getShort(12).toInt() and 0xFFFF
            val bits = pointer.getShort(14).toInt() and 0xFFFF
            val subtype = if (tag == WAVE_FORMAT_EXTENSIBLE) pointer.getInt(24) else tag
            val isFloat = subtype == WAVE_FORMAT_IEEE_FLOAT
            require(subtype == WAVE_FORMAT_PCM || isFloat) {
                "Unsupported Windows mix format: tag=$tag subtype=$subtype"
            }
            require(channels > 0 && sampleRate > 0 && blockAlign > 0) {
                "Invalid Windows mix format"
            }
            require((isFloat && bits == 32) || (!isFloat && bits in setOf(16, 24, 32))) {
                "Unsupported Windows sample format: ${bits}-bit"
            }
            return WaveFormat(tag, channels, sampleRate, blockAlign, bits, isFloat)
        }
    }
}

private class PcmFrameConverter(
    private val format: WaveFormat,
) {
    private val outputFrame = ByteArray(AUDIO_FRAME_BYTES)
    private var outputOffset = 0
    private var samplePhase = 0

    fun accept(input: ByteArray, onFrame: (ByteArray) -> Unit) {
        val frameCount = input.size / format.blockAlign
        repeat(frameCount) { frameIndex ->
            var mono = 0f
            repeat(format.channels) { channel ->
                val offset = frameIndex * format.blockAlign + channel * (format.bitsPerSample / 8)
                mono += readSample(input, offset)
            }
            mono /= format.channels
            samplePhase += AUDIO_SAMPLE_RATE
            while (samplePhase >= format.sampleRate) {
                samplePhase -= format.sampleRate
                appendSample(mono, onFrame)
            }
        }
    }

    private fun readSample(input: ByteArray, offset: Int): Float {
        return if (format.isFloat) {
            Float.fromBits(readIntLittleEndian(input, offset)).coerceIn(-1f, 1f)
        } else {
            when (format.bitsPerSample) {
                16 -> readShortLittleEndian(input, offset) / 32_768f
                24 -> readInt24LittleEndian(input, offset) / 8_388_608f
                32 -> readIntLittleEndian(input, offset) / 2_147_483_648f
                else -> 0f
            }
        }
    }

    private fun appendSample(sample: Float, onFrame: (ByteArray) -> Unit) {
        val value = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        outputFrame[outputOffset++] = (value.toInt() and 0xFF).toByte()
        outputFrame[outputOffset++] = (value.toInt() shr 8 and 0xFF).toByte()
        if (outputOffset == outputFrame.size) {
            onFrame(outputFrame.copyOf())
            outputOffset = 0
        }
    }

    private fun readShortLittleEndian(input: ByteArray, offset: Int): Int {
        val value = (input[offset].toInt() and 0xFF) or ((input[offset + 1].toInt() and 0xFF) shl 8)
        return value.toShort().toInt()
    }

    private fun readInt24LittleEndian(input: ByteArray, offset: Int): Int {
        var value = (input[offset].toInt() and 0xFF) or
            ((input[offset + 1].toInt() and 0xFF) shl 8) or
            ((input[offset + 2].toInt() and 0xFF) shl 16)
        if (value and 0x800000 != 0) value = value or -0x1000000
        return value
    }

    private fun readIntLittleEndian(input: ByteArray, offset: Int): Int {
        return (input[offset].toInt() and 0xFF) or
            ((input[offset + 1].toInt() and 0xFF) shl 8) or
            ((input[offset + 2].toInt() and 0xFF) shl 16) or
            ((input[offset + 3].toInt() and 0xFF) shl 24)
    }
}

private fun checkResult(
    result: HRESULT,
    allowFalse: Boolean = false,
    operation: String,
) {
    if (COMUtils.FAILED(result) || (!allowFalse && result.toInt() != 0)) {
        throw AudioCaptureException("$operation failed with HRESULT 0x${result.toInt().toUInt().toString(16)}")
    }
}
