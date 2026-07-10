package com.example.interviewassistant.desktop.feature.interviewassistant

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Desktop two-column dashboard for resumes and interview history.
 */
@Composable
fun DesktopAssistantDashboard(
    resumeState: ResumeLibraryUiState,
    historyState: SessionHistoryUiState,
    strings: StringsProvider,
    onResumeEvent: (ResumeLibraryUiEvent) -> Unit,
    onHistoryEvent: (SessionHistoryUiEvent) -> Unit,
    onStartSession: (Resume) -> Unit,
    onOpenSession: (InterviewSession) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var viewingResumeId by remember { mutableStateOf<String?>(null) }
    val viewingResume = resumeState.resumes.firstOrNull { it.id == viewingResumeId }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize().padding(AppDesign.spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.xl),
        ) {
            DesktopListSection(
                modifier = Modifier.weight(1f),
                title = strings.get(AppStringId.RESUME_LIBRARY_TITLE),
                action = {
                    Button(
                        enabled = resumeState.importingFileName == null,
                        onClick = {
                            val file = chooseResume(strings.get(AppStringId.IMPORT_RESUME)) ?: return@Button
                            scope.launch(Dispatchers.IO) {
                                onResumeEvent(file.toImportEvent())
                            }
                        },
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
                },
            ) {
                resumeState.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (!resumeState.isLoading && resumeState.resumes.isEmpty()) {
                    item { Text(strings.get(AppStringId.RESUME_EMPTY)) }
                }
                items(resumeState.resumes, key = Resume::id) { resume ->
                    DesktopResumeCard(
                        resume = resume,
                        isProcessing = resume.isProcessing(resumeState.processingResumeIds),
                        strings = strings,
                        onStart = { onStartSession(resume) },
                        onViewContent = { viewingResumeId = resume.id },
                        onRetry = { onResumeEvent(ResumeLibraryUiEvent.RetryOcr(resume.id)) },
                        onDelete = { onResumeEvent(ResumeLibraryUiEvent.Delete(resume.id)) },
                    )
                }
            }
            DesktopListSection(
                modifier = Modifier.weight(1f),
                title = strings.get(AppStringId.RECENT_SESSIONS_TITLE),
            ) {
                if (!historyState.isLoading && historyState.sessions.isEmpty()) {
                    item { Text(strings.get(AppStringId.SESSION_EMPTY)) }
                }
                items(historyState.sessions, key = InterviewSession::id) { session ->
                    DesktopSessionCard(
                        session = session,
                        strings = strings,
                        onOpen = { onOpenSession(session) },
                        onDelete = { onHistoryEvent(SessionHistoryUiEvent.Delete(session.id)) },
                    )
                }
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
                DesktopResumeContentScreen(
                    resume = resume,
                    strings = strings,
                    onNavigateBack = { viewingResumeId = null },
                )
            }
        }
    }
}

@Composable
private fun DesktopListSection(
    modifier: Modifier,
    title: String,
    action: @Composable (() -> Unit)? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = AppDesign.typography.pageTitle)
            action?.invoke()
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.lg),
            content = content,
        )
    }
}

@Composable
private fun DesktopResumeCard(
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
                    Button(onClick = onStart) { Text(strings.get(AppStringId.START_SESSION)) }
                    if (!ocrText.isNullOrBlank()) {
                        TextButton(onClick = onViewContent) {
                            Text(strings.get(AppStringId.RESUME_VIEW_CONTENT))
                        }
                    }
                }
                if (resume.ocrStatus == OcrStatus.FAILED && !isProcessing) {
                    TextButton(onClick = onRetry) { Text(strings.get(AppStringId.COMMON_RETRY)) }
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
private fun DesktopResumeContentScreen(
    resume: Resume,
    strings: StringsProvider,
    onNavigateBack: () -> Unit,
) {
    val fileStore = remember { org.koin.core.context.GlobalContext.get().get<com.example.interviewassistant.core.file.AppFileStore>() }
    val content = resume.ocrText.orEmpty()
    var renderHtml by remember { mutableStateOf(content.looksLikeHtml()) }
    var copied by remember { mutableStateOf(false) }
    var assetBaseUri by remember { mutableStateOf<String?>(null) }
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
                            copyTextToClipboard(content)
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
                .padding(AppDesign.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Text(resume.displayName, style = AppDesign.typography.sectionTitle)
            if (renderHtml) {
                DesktopHtmlPreview(
                    html = OcrDisplayHtml.document(content, assetBaseUri),
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
private fun DesktopHtmlPreview(
    html: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.ui.awt.SwingPanel(
        modifier = modifier,
        factory = {
            javax.swing.JEditorPane().apply {
                contentType = "text/html"
                isEditable = false
                text = html
            }.let { pane ->
                javax.swing.JScrollPane(pane)
            }
        },
        update = { scrollPane ->
            val pane = (scrollPane as javax.swing.JScrollPane).viewport.view as javax.swing.JEditorPane
            if (pane.text != html) {
                pane.text = html
                pane.caretPosition = 0
            }
        },
    )
}

private fun copyTextToClipboard(text: String) {
    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
}

private fun String.looksLikeHtml(): Boolean {
    val sample = take(2_000).lowercase()
    return listOf("<html", "<div", "<table", "<p", "<span", "<br", "<img", "\\underline", "$").any(sample::contains) ||
        sample.lineSequence().any { it.trimStart().startsWith("# ") }
}

@Composable
private fun DesktopSessionCard(
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
            Text(
                session.title,
                style = AppDesign.typography.itemTitle,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpen) { Text(strings.get(AppStringId.OPEN_SESSION)) }
            TextButton(onClick = onDelete) { Text(strings.get(AppStringId.COMMON_DELETE)) }
        }
    }
}

private fun chooseResume(title: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isMultipleMode = false
    dialog.isVisible = true
    return dialog.files.firstOrNull()
}

private fun File.toImportEvent(): ResumeLibraryUiEvent.Import {
    val mimeType = when (extension.lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        else -> "image/jpeg"
    }
    return ResumeLibraryUiEvent.Import(
        displayName = nameWithoutExtension,
        fileName = name,
        mimeType = mimeType,
        content = readBytes(),
    )
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
