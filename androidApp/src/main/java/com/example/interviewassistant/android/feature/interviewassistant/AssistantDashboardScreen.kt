package com.example.interviewassistant.android.feature.interviewassistant

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.core.design.theme.AppDesign
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.OcrDisplayHtml
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSession
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.Resume
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ResumeLibraryUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ResumeLibraryUiState
import com.example.interviewassistant.feature.interviewassistant.presentation.state.SessionHistoryUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.SessionHistoryUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android dashboard containing resume OCR status and recoverable session history.
 */
@Composable
fun AssistantDashboardScreen(
    resumeState: ResumeLibraryUiState,
    historyState: SessionHistoryUiState,
    strings: StringsProvider,
    onResumeEvent: (ResumeLibraryUiEvent) -> Unit,
    onHistoryEvent: (SessionHistoryUiEvent) -> Unit,
    onStartSession: (Resume) -> Unit,
    onOpenSession: (InterviewSession) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewingResumeId by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    val viewingResume = resumeState.resumes.firstOrNull { it.id == viewingResumeId }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val document = withContext(Dispatchers.IO) { context.readDocument(uri) }
                    importError = null
                    onResumeEvent(
                        ResumeLibraryUiEvent.Import(
                            displayName = document.fileName.substringBeforeLast('.'),
                            fileName = document.fileName,
                            mimeType = document.mimeType,
                            content = document.content,
                        ),
                    )
                } catch (_: Throwable) {
                    importError = strings.get(AppStringId.ERROR_GENERIC)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(strings.get(AppStringId.RESUME_LIBRARY_TITLE), style = AppDesign.typography.pageTitle)
                    Button(
                        enabled = resumeState.importingFileName == null,
                        onClick = { picker.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) },
                    ) {
                        if (resumeState.importingFileName != null) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = AppDesign.spacing.sm)
                                    .size(AppDesign.spacing.lg),
                                strokeWidth = AppDesign.spacing.xxs,
                            )
                        }
                        Text(strings.get(AppStringId.IMPORT_RESUME))
                    }
                }
            }
            resumeState.errorMessage?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
            importError?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
            if (!resumeState.isLoading && resumeState.resumes.isEmpty()) {
                item { Text(strings.get(AppStringId.RESUME_EMPTY)) }
            }
            items(resumeState.resumes, key = Resume::id) { resume ->
                ResumeCard(
                    resume = resume,
                    isProcessing = resume.isProcessing(resumeState.processingResumeIds),
                    strings = strings,
                    onStart = { onStartSession(resume) },
                    onViewContent = { viewingResumeId = resume.id },
                    onRetry = { onResumeEvent(ResumeLibraryUiEvent.RetryOcr(resume.id)) },
                    onDelete = { onResumeEvent(ResumeLibraryUiEvent.Delete(resume.id)) },
                )
            }
            item {
                Text(
                    text = strings.get(AppStringId.RECENT_SESSIONS_TITLE),
                    style = AppDesign.typography.sectionTitle,
                    modifier = Modifier.padding(top = AppDesign.spacing.lg),
                )
            }
            if (!historyState.isLoading && historyState.sessions.isEmpty()) {
                item { Text(strings.get(AppStringId.SESSION_EMPTY)) }
            }
            items(historyState.sessions, key = InterviewSession::id) { session ->
                SessionCard(
                    session = session,
                    strings = strings,
                    onOpen = { onOpenSession(session) },
                    onDelete = { onHistoryEvent(SessionHistoryUiEvent.Delete(session.id)) },
                )
            }
        }

        AnimatedVisibility(
            visible = viewingResume != null,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300),
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300),
            ) + fadeOut(animationSpec = tween(300)),
        ) {
            viewingResume?.let { resume ->
                ResumeContentScreen(
                    resume = resume,
                    strings = strings,
                    onNavigateBack = { viewingResumeId = null },
                )
            }
        }
    }
}

@Composable
private fun ResumeCard(
    resume: Resume,
    isProcessing: Boolean,
    strings: StringsProvider,
    onStart: () -> Unit,
    onViewContent: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppDesign.colors.surfaceMuted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Text(resume.displayName, style = AppDesign.typography.sectionTitle)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDesign.spacing.lg),
                        strokeWidth = AppDesign.spacing.xxs,
                    )
                }
                Text(
                    text = if (isProcessing) {
                        strings.get(AppStringId.OCR_PROCESSING_HINT)
                    } else {
                        strings.get(resume.ocrStatus.stringId())
                    },
                    style = AppDesign.typography.caption,
                    color = AppDesign.colors.textSecondary,
                )
            }
            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            resume.ocrError?.takeIf { !isProcessing }?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = AppDesign.typography.caption)
            }
            val ocrText = resume.ocrText
            if (resume.ocrStatus == OcrStatus.READY && !ocrText.isNullOrBlank()) {
                Text(
                    text = OcrDisplayHtml.plainPreview(ocrText),
                    style = AppDesign.typography.body,
                    color = AppDesign.colors.textSecondary,
                    maxLines = 3,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm)) {
                if (resume.ocrStatus == OcrStatus.READY) {
                    Button(onClick = onStart) {
                        Text(strings.get(AppStringId.START_SESSION))
                    }
                    if (!ocrText.isNullOrBlank()) {
                        TextButton(onClick = onViewContent) {
                            Text(strings.get(AppStringId.RESUME_VIEW_CONTENT))
                        }
                    }
                }
                if (resume.ocrStatus == OcrStatus.FAILED && !isProcessing) {
                    TextButton(onClick = onRetry) {
                        Text(strings.get(AppStringId.COMMON_RETRY))
                    }
                }
                TextButton(onClick = onDelete, enabled = !isProcessing) {
                    Text(strings.get(AppStringId.COMMON_DELETE))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResumeContentScreen(
    resume: Resume,
    strings: StringsProvider,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val fileStore = remember { org.koin.core.context.GlobalContext.get().get<com.example.interviewassistant.core.file.AppFileStore>() }
    val content = resume.ocrText.orEmpty()
    var renderHtml by remember { mutableStateOf(content.looksLikeHtml()) }
    var copied by remember { mutableStateOf(false) }
    var assetBaseUri by remember { mutableStateOf<String?>(null) }
    BackHandler { onNavigateBack() }
    LaunchedEffect(resume.id) {
        assetBaseUri = runCatching { fileStore.ocrAssetBaseUri(resume.id) }.getOrNull()
    }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1_500)
            copied = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(strings.get(AppStringId.RESUME_CONTENT_TITLE)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(strings.get(AppStringId.COMMON_BACK))
                    }
                },
                actions = {
                    TextButton(onClick = { renderHtml = !renderHtml }) {
                        Text(
                            strings.get(
                                if (renderHtml) AppStringId.RESUME_SHOW_SOURCE else AppStringId.RESUME_RENDER_HTML,
                            ),
                        )
                    }
                    TextButton(
                        onClick = {
                            copyTextToClipboard(context, content)
                            copied = true
                        },
                    ) {
                        Text(
                            strings.get(
                                if (copied) AppStringId.RESUME_COPIED else AppStringId.RESUME_COPY,
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Text(resume.displayName, style = AppDesign.typography.sectionTitle)
            if (renderHtml) {
                AndroidHtmlPreview(
                    html = OcrDisplayHtml.document(content, assetBaseUri),
                    baseUri = assetBaseUri,
                    modifier = Modifier.fillMaxSize().weight(1f),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(text = content, style = AppDesign.typography.body)
                }
            }
        }
    }
}

@Composable
private fun AndroidHtmlPreview(
    html: String,
    baseUri: String?,
    modifier: Modifier = Modifier,
) {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                loadDataWithBaseURL(baseUri, html, "text/html", "utf-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(baseUri, html, "text/html", "utf-8", null)
        },
    )
}

private fun copyTextToClipboard(context: android.content.Context, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("resume", text))
}

private fun String.looksLikeHtml(): Boolean {
    val sample = take(2_000).lowercase()
    return listOf("<html", "<div", "<table", "<p", "<span", "<br", "<img", "\\underline", "$").any(sample::contains) ||
        sample.lineSequence().any { it.trimStart().startsWith("# ") }
}

@Composable
private fun SessionCard(
    session: InterviewSession,
    strings: StringsProvider,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppDesign.colors.surfaceMuted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.title, style = AppDesign.typography.itemTitle)
            }
            TextButton(onClick = onOpen) { Text(strings.get(AppStringId.OPEN_SESSION)) }
            TextButton(onClick = onDelete) { Text(strings.get(AppStringId.COMMON_DELETE)) }
        }
    }
}

private data class ImportedDocument(
    val fileName: String,
    val mimeType: String,
    val content: ByteArray,
)

private fun android.content.Context.readDocument(uri: Uri): ImportedDocument {
    val fileName = contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: "resume"
    val mimeType = contentResolver.getType(uri) ?: when (fileName.substringAfterLast('.').lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        else -> "image/jpeg"
    }
    val content = requireNotNull(contentResolver.openInputStream(uri)) { "Unable to open selected resume" }
        .use { it.readBytes() }
    return ImportedDocument(fileName, mimeType, content)
}

private fun Resume.isProcessing(processingResumeIds: Set<String>): Boolean {
    return id in processingResumeIds || ocrStatus in PROCESSING_STATUSES
}

private fun OcrStatus.stringId(): AppStringId = when (this) {
    OcrStatus.QUEUED -> AppStringId.OCR_QUEUED
    OcrStatus.PENDING -> AppStringId.OCR_PENDING
    OcrStatus.RUNNING -> AppStringId.OCR_RUNNING
    OcrStatus.READY -> AppStringId.OCR_READY
    OcrStatus.FAILED -> AppStringId.OCR_FAILED
}

private val PROCESSING_STATUSES = setOf(OcrStatus.QUEUED, OcrStatus.PENDING, OcrStatus.RUNNING)
