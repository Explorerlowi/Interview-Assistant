@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class,
)

package com.example.interviewassistant.desktop.feature.interviewassistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

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
    var importError by remember { mutableStateOf<String?>(null) }
    var isDragOver by remember { mutableStateOf(false) }
    val viewingResume = resumeState.resumes.firstOrNull { it.id == viewingResumeId }
    val importEnabled = resumeState.importingFileName == null

    fun submitImport(event: ResumeLibraryUiEvent.Import) {
        importError = null
        onResumeEvent(event)
    }

    fun importFile(file: File) {
        if (!importEnabled) return
        if (!file.isSupportedResume()) {
            importError = strings.get(AppStringId.IMPORT_RESUME_UNSUPPORTED)
            return
        }
        scope.launch(Dispatchers.IO) {
            runCatching { file.toImportEvent() }
                .onSuccess { event ->
                    withContext(Dispatchers.Main) { submitImport(event) }
                }
                .onFailure {
                    withContext(Dispatchers.Main) {
                        importError = strings.get(AppStringId.ERROR_GENERIC)
                    }
                }
        }
    }

    fun importFiles(files: List<File>) {
        val supported = files.filter { it.isFile && it.isSupportedResume() }
        if (supported.isEmpty()) {
            importError = strings.get(AppStringId.IMPORT_RESUME_UNSUPPORTED)
            return
        }
        supported.forEach(::importFile)
    }

    fun pasteFromClipboard() {
        if (!importEnabled) return
        scope.launch(Dispatchers.IO) {
            val result = readClipboardResume()
            withContext(Dispatchers.Main) {
                when (result) {
                    is ClipboardResumeResult.FileImport -> submitImport(result.event)
                    is ClipboardResumeResult.Unsupported -> {
                        importError = strings.get(AppStringId.IMPORT_RESUME_UNSUPPORTED)
                    }
                    ClipboardResumeResult.Empty -> {
                        importError = strings.get(AppStringId.IMPORT_RESUME_CLIPBOARD_EMPTY)
                    }
                }
            }
        }
    }

    fun openFilePicker() {
        if (!importEnabled) return
        val file = chooseResume(strings.get(AppStringId.IMPORT_RESUME)) ?: return
        importFile(file)
    }

    val importEnabledState = rememberUpdatedState(importEnabled)
    val onFilesDropped = rememberUpdatedState<(List<File>) -> Unit> { files -> importFiles(files) }
    val onUnsupportedDrop = rememberUpdatedState {
        importError = strings.get(AppStringId.IMPORT_RESUME_UNSUPPORTED)
    }

    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                if (importEnabledState.value) isDragOver = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragOver = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragOver = false
                if (!importEnabledState.value) return false
                val files = event.awtTransferable.resumeFilesOrNull()
                return if (files != null) {
                    onFilesDropped.value(files)
                    true
                } else {
                    onUnsupportedDrop.value()
                    false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                if (
                    keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.key == Key.V &&
                    (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)
                ) {
                    pasteFromClipboard()
                    true
                } else {
                    false
                }
            },
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(AppDesign.spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.xl),
        ) {
            DesktopListSection(
                modifier = Modifier.weight(1f),
                title = strings.get(AppStringId.RESUME_LIBRARY_TITLE),
                action = {
                    Button(
                        enabled = importEnabled,
                        onClick = ::openFilePicker,
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
                item {
                    ResumeImportDropZone(
                        enabled = importEnabled,
                        isDragOver = isDragOver,
                        hint = strings.get(
                            if (isDragOver) {
                                AppStringId.IMPORT_RESUME_DROP_ACTIVE
                            } else {
                                AppStringId.IMPORT_RESUME_HINT
                            },
                        ),
                        pasteLabel = strings.get(AppStringId.IMPORT_RESUME_PASTE),
                        onClick = ::openFilePicker,
                        onPasteClick = ::pasteFromClipboard,
                        dropTarget = dropTarget,
                    )
                }
                resumeState.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                importError?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
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
                    onSaveOcrText = { text ->
                        onResumeEvent(
                            ResumeLibraryUiEvent.UpdateOcrText(
                                resumeId = resume.id,
                                ocrText = text,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ResumeImportDropZone(
    enabled: Boolean,
    isDragOver: Boolean,
    hint: String,
    pasteLabel: String,
    onClick: () -> Unit,
    onPasteClick: () -> Unit,
    dropTarget: DragAndDropTarget,
) {
    val borderColor = when {
        isDragOver -> MaterialTheme.colorScheme.primary
        else -> AppDesign.colors.border
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), shape = MaterialTheme.shapes.medium)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    enabled && event.awtTransferable.isResumeDropSupported()
                },
                target = dropTarget,
            )
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragOver) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                AppDesign.colors.surfaceMuted
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
        ) {
            Text(
                text = hint,
                style = AppDesign.typography.body,
                color = AppDesign.colors.textSecondary,
            )
            TextButton(
                enabled = enabled,
                onClick = onPasteClick,
            ) {
                Text(pasteLabel)
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
    onSaveOcrText: (String) -> Unit,
) {
    val fileStore = remember { org.koin.core.context.GlobalContext.get().get<com.example.interviewassistant.core.file.AppFileStore>() }
    val privacyRedactor = remember {
        org.koin.core.context.GlobalContext.get()
            .get<com.example.interviewassistant.feature.interviewassistant.domain.usecase.PrivacyRedactor>()
    }
    val savedContent = resume.ocrText.orEmpty()
    val originalContent = resume.ocrOriginalText.orEmpty().ifBlank { savedContent }
    var draftText by remember(resume.id, savedContent) { mutableStateOf(savedContent) }
    var isEditing by remember(resume.id) { mutableStateOf(false) }
    var showOriginal by remember(resume.id) { mutableStateOf(false) }
    var renderHtml by remember(resume.id) { mutableStateOf(savedContent.looksLikeHtml()) }
    var redactPreview by remember(resume.id) { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var assetBaseUri by remember { mutableStateOf<String?>(null) }
    val isDirty = draftText != savedContent
    val baseContent = when {
        isEditing -> draftText
        showOriginal -> originalContent
        else -> draftText
    }
    val displayContent = remember(baseContent, redactPreview, isEditing) {
        if (!isEditing && redactPreview) privacyRedactor.redact(baseContent) else baseContent
    }

    fun exitEditing(discard: Boolean) {
        if (discard) {
            draftText = savedContent
        }
        isEditing = false
        redactPreview = false
    }

    fun handleBack() {
        if (isEditing) {
            exitEditing(discard = true)
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(resume.id) {
        assetBaseUri = runCatching { fileStore.ocrAssetBaseUri(resume.id) }.getOrNull()
    }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1_500)
            copied = false
        }
    }
    LaunchedEffect(savedContent, isEditing) {
        if (!isEditing) {
            draftText = savedContent
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(strings.get(AppStringId.RESUME_CONTENT_TITLE)) },
                navigationIcon = {
                    TextButton(onClick = ::handleBack) {
                        Text(strings.get(AppStringId.COMMON_BACK))
                    }
                },
                actions = {
                    if (isEditing) {
                        if (originalContent.isNotBlank()) {
                            TextButton(
                                onClick = { draftText = originalContent },
                                enabled = draftText != originalContent,
                            ) {
                                Text(strings.get(AppStringId.RESUME_RESTORE_ORIGINAL))
                            }
                        }
                        TextButton(onClick = { exitEditing(discard = true) }) {
                            Text(strings.get(AppStringId.COMMON_CANCEL))
                        }
                        TextButton(
                            enabled = isDirty,
                            onClick = {
                                onSaveOcrText(draftText)
                                isEditing = false
                                showOriginal = false
                                redactPreview = false
                            },
                        ) {
                            Text(strings.get(AppStringId.COMMON_SAVE))
                        }
                    } else {
                        TextButton(
                            onClick = {
                                isEditing = true
                                showOriginal = false
                                renderHtml = false
                                redactPreview = false
                                draftText = savedContent
                            },
                        ) {
                            Text(strings.get(AppStringId.RESUME_EDIT))
                        }
                        if (originalContent.isNotBlank()) {
                            TextButton(onClick = { showOriginal = !showOriginal }) {
                                Text(
                                    strings.get(
                                        if (showOriginal) {
                                            AppStringId.RESUME_VIEW_CURRENT
                                        } else {
                                            AppStringId.RESUME_VIEW_ORIGINAL
                                        },
                                    ),
                                )
                            }
                        }
                        TextButton(onClick = { redactPreview = !redactPreview }) {
                            Text(
                                strings.get(
                                    if (redactPreview) {
                                        AppStringId.RESUME_REDACT_ORIGINAL
                                    } else {
                                        AppStringId.RESUME_REDACT_PREVIEW
                                    },
                                ),
                            )
                        }
                        TextButton(onClick = { renderHtml = !renderHtml }) {
                            Text(
                                strings.get(
                                    if (renderHtml) {
                                        AppStringId.RESUME_SHOW_SOURCE
                                    } else {
                                        AppStringId.RESUME_RENDER_HTML
                                    },
                                ),
                            )
                        }
                        TextButton(
                            onClick = {
                                copyTextToClipboard(displayContent)
                                copied = true
                            },
                        ) {
                            Text(
                                strings.get(
                                    if (copied) AppStringId.RESUME_COPIED else AppStringId.RESUME_COPY,
                                ),
                            )
                        }
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
            if (!isEditing && showOriginal) {
                Text(
                    text = strings.get(AppStringId.RESUME_VIEW_ORIGINAL),
                    style = AppDesign.typography.caption,
                    color = AppDesign.colors.textSecondary,
                )
            }
            when {
                isEditing -> {
                    androidx.compose.material3.OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        textStyle = AppDesign.typography.body,
                    )
                }
                renderHtml -> {
                    DesktopHtmlPreview(
                        html = OcrDisplayHtml.document(displayContent, assetBaseUri),
                        modifier = Modifier.fillMaxSize().weight(1f),
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(text = displayContent, style = AppDesign.typography.body)
                    }
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
    dialog.setFilenameFilter { _, name -> name.isSupportedResumeFileName() }
    dialog.isVisible = true
    return dialog.files.firstOrNull()?.takeIf { it.isSupportedResume() }
}

private fun File.toImportEvent(): ResumeLibraryUiEvent.Import {
    val mimeType = mimeTypeForResumeExtension(extension)
    return ResumeLibraryUiEvent.Import(
        displayName = nameWithoutExtension.ifBlank { "resume" },
        fileName = name,
        mimeType = mimeType,
        content = readBytes(),
    )
}

private fun File.isSupportedResume(): Boolean = isFile && name.isSupportedResumeFileName()

private fun String.isSupportedResumeFileName(): Boolean {
    return substringAfterLast('.', missingDelimiterValue = "").lowercase() in SUPPORTED_RESUME_EXTENSIONS
}

private fun mimeTypeForResumeExtension(extension: String): String {
    return when (extension.lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        else -> "image/jpeg"
    }
}

private fun java.awt.datatransfer.Transferable.isResumeDropSupported(): Boolean {
    return isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
        isDataFlavorSupported(DataFlavor.imageFlavor)
}

@Suppress("UNCHECKED_CAST")
private fun java.awt.datatransfer.Transferable.resumeFilesOrNull(): List<File>? {
    if (!isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return null
    return (getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
        ?.filterIsInstance<File>()
        ?.filter { it.isSupportedResume() }
        ?.takeIf { it.isNotEmpty() }
}

private sealed interface ClipboardResumeResult {
    data class FileImport(val event: ResumeLibraryUiEvent.Import) : ClipboardResumeResult
    data object Unsupported : ClipboardResumeResult
    data object Empty : ClipboardResumeResult
}

@Suppress("UNCHECKED_CAST")
private fun readClipboardResume(): ClipboardResumeResult {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val contents = runCatching { clipboard.getContents(null) }.getOrNull() ?: return ClipboardResumeResult.Empty

    if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        val files = (contents.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
            ?.filterIsInstance<File>()
            .orEmpty()
        val supported = files.filter { it.isSupportedResume() }
        return when {
            supported.isNotEmpty() -> ClipboardResumeResult.FileImport(supported.first().toImportEvent())
            files.isNotEmpty() -> ClipboardResumeResult.Unsupported
            else -> ClipboardResumeResult.Empty
        }
    }

    if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
        val image = contents.getTransferData(DataFlavor.imageFlavor) as? Image
            ?: return ClipboardResumeResult.Empty
        val bytes = image.toPngBytes() ?: return ClipboardResumeResult.Empty
        val fileName = "clipboard-paste.png"
        return ClipboardResumeResult.FileImport(
            ResumeLibraryUiEvent.Import(
                displayName = "clipboard-paste",
                fileName = fileName,
                mimeType = "image/png",
                content = bytes,
            ),
        )
    }

    return ClipboardResumeResult.Empty
}

private fun Image.toPngBytes(): ByteArray? {
    val buffered = when (this) {
        is BufferedImage -> this
        else -> {
            val width = getWidth(null).takeIf { it > 0 } ?: return null
            val height = getHeight(null).takeIf { it > 0 } ?: return null
            BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { canvas ->
                val graphics = canvas.createGraphics()
                graphics.drawImage(this, 0, 0, null)
                graphics.dispose()
            }
        }
    }
    return ByteArrayOutputStream().use { output ->
        if (!ImageIO.write(buffered, "png", output)) return null
        output.toByteArray()
    }
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
private val SUPPORTED_RESUME_EXTENSIONS = setOf("pdf", "jpg", "jpeg", "png")

