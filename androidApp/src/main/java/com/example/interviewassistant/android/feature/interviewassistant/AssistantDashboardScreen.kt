package com.example.interviewassistant.android.feature.interviewassistant

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.android.R
import com.example.interviewassistant.core.design.theme.AppDesign
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.OcrDisplayHtml
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSession
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.Resume
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.PrivacyRedactor
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
@OptIn(ExperimentalFoundationApi::class)
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
    var isDragOver by remember { mutableStateOf(false) }
    val viewingResume = resumeState.resumes.firstOrNull { it.id == viewingResumeId }
    val importEnabled = resumeState.importingFileName == null

    fun submitImport(document: ImportedDocument) {
        importError = null
        onResumeEvent(
            ResumeLibraryUiEvent.Import(
                displayName = document.fileName.substringBeforeLast('.').ifBlank { "resume" },
                fileName = document.fileName,
                mimeType = document.mimeType,
                content = document.content,
            ),
        )
    }

    fun importUri(uri: Uri) {
        if (!importEnabled) return
        scope.launch {
            try {
                val document = withContext(Dispatchers.IO) { context.readDocument(uri) }
                if (!document.isSupportedResume()) {
                    importError = strings.get(AppStringId.IMPORT_RESUME_UNSUPPORTED)
                    return@launch
                }
                submitImport(document)
            } catch (_: Throwable) {
                importError = strings.get(AppStringId.ERROR_GENERIC)
            }
        }
    }

    fun importUris(uris: List<Uri>) {
        val unique = uris.distinct()
        if (unique.isEmpty()) {
            importError = strings.get(AppStringId.IMPORT_RESUME_UNSUPPORTED)
            return
        }
        unique.forEach(::importUri)
    }

    fun pasteFromClipboard() {
        if (!importEnabled) return
        scope.launch {
            val result = withContext(Dispatchers.IO) { context.readClipboardResume() }
            when (result) {
                is ClipboardResumeResult.Documents -> result.documents.forEach(::submitImport)
                ClipboardResumeResult.Unsupported -> {
                    importError = strings.get(AppStringId.IMPORT_RESUME_UNSUPPORTED)
                }
                ClipboardResumeResult.Empty -> {
                    importError = strings.get(AppStringId.IMPORT_RESUME_CLIPBOARD_EMPTY)
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importUri(uri)
    }

    val importEnabledState = rememberUpdatedState(importEnabled)
    val onUrisDropped = rememberUpdatedState<(List<Uri>) -> Unit> { uris -> importUris(uris) }
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
                val uris = event.toAndroidDragEvent().clipData.resumeUris()
                return if (uris.isNotEmpty()) {
                    onUrisDropped.value(uris)
                    true
                } else {
                    onUnsupportedDrop.value()
                    false
                }
            }
        }
    }

    val receiveContentListener = remember {
        ReceiveContentListener { transferableContent ->
            if (!importEnabledState.value) return@ReceiveContentListener transferableContent
            val uris = transferableContent.clipEntry?.clipData?.resumeUris().orEmpty()
            if (uris.isEmpty()) return@ReceiveContentListener transferableContent
            onUrisDropped.value(uris)
            null
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppDesign.spacing.lg,
                top = AppDesign.spacing.xl,
                end = AppDesign.spacing.lg,
                bottom = AppDesign.spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.xs)) {
                        Text(
                            strings.get(AppStringId.ASSISTANT_TITLE),
                            style = AppDesign.typography.pageTitle,
                        )
                        Text(
                            strings.get(AppStringId.RESUME_LIBRARY_TITLE),
                            style = AppDesign.typography.body,
                            color = AppDesign.colors.textSecondary,
                        )
                    }
                    Button(
                        enabled = importEnabled,
                        onClick = {
                            picker.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
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
                }
            }
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
                    onClick = {
                        picker.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
                    },
                    onPasteClick = ::pasteFromClipboard,
                    dropTarget = dropTarget,
                    receiveContentListener = receiveContentListener,
                )
            }
            resumeState.errorMessage?.let { message ->
                item {
                    InlineErrorMessage(message)
                }
            }
            importError?.let { message ->
                item {
                    InlineErrorMessage(message)
                }
            }
            if (resumeState.isLoading && resumeState.resumes.isEmpty()) {
                item {
                    LoadingState()
                }
            }
            if (!resumeState.isLoading && resumeState.resumes.isEmpty()) {
                item {
                    EmptyState(
                        message = strings.get(AppStringId.RESUME_EMPTY),
                        iconRes = R.drawable.ic_description,
                    )
                }
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
                SectionHeader(
                    title = strings.get(AppStringId.RECENT_SESSIONS_TITLE),
                    count = historyState.sessions.size,
                    modifier = Modifier.padding(top = AppDesign.spacing.lg),
                )
            }
            if (historyState.isLoading && historyState.sessions.isEmpty()) {
                item { LoadingState() }
            }
            if (!historyState.isLoading && historyState.sessions.isEmpty()) {
                item {
                    EmptyState(
                        message = strings.get(AppStringId.SESSION_EMPTY),
                        iconRes = R.drawable.ic_assistant,
                    )
                }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResumeImportDropZone(
    enabled: Boolean,
    isDragOver: Boolean,
    hint: String,
    pasteLabel: String,
    onClick: () -> Unit,
    onPasteClick: () -> Unit,
    dropTarget: DragAndDropTarget,
    receiveContentListener: ReceiveContentListener,
) {
    val borderColor = when {
        isDragOver -> MaterialTheme.colorScheme.primary
        else -> AppDesign.colors.border
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), shape = MaterialTheme.shapes.large)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    enabled && event.toAndroidDragEvent().clipDescription?.hasResumeMime() == true
                },
                target = dropTarget,
            )
            .contentReceiver(receiveContentListener)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragOver) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                AppDesign.colors.brandSubtle.copy(alpha = 0.55f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDesign.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = AppDesign.colors.surface.copy(alpha = 0.8f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_description),
                    contentDescription = null,
                    modifier = Modifier.padding(AppDesign.spacing.md).size(24.dp),
                    tint = AppDesign.colors.brand,
                )
            }
            Text(
                text = hint,
                modifier = Modifier.weight(1f),
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
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = AppDesign.typography.sectionTitle)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = AppDesign.colors.surfaceMuted,
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(
                    horizontal = AppDesign.spacing.sm,
                    vertical = AppDesign.spacing.xs,
                ),
                style = AppDesign.typography.caption,
                color = AppDesign.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun EmptyState(
    message: String,
    iconRes: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = AppDesign.colors.surface,
        border = BorderStroke(1.dp, AppDesign.colors.divider),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = AppDesign.colors.textTertiary,
            )
            Text(
                text = message,
                style = AppDesign.typography.body,
                color = AppDesign.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun InlineErrorMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(AppDesign.spacing.md),
            style = AppDesign.typography.body,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
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
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = AppDesign.colors.surface),
        border = BorderStroke(1.dp, AppDesign.colors.divider),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = AppDesign.colors.surfaceMuted,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_description),
                        contentDescription = null,
                        modifier = Modifier.padding(AppDesign.spacing.sm).size(24.dp),
                        tint = AppDesign.colors.brand,
                    )
                }
                Text(
                    text = resume.displayName,
                    modifier = Modifier.weight(1f),
                    style = AppDesign.typography.sectionTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                OcrStatusPill(
                    status = resume.ocrStatus,
                    isProcessing = isProcessing,
                    label = strings.get(
                        if (isProcessing) AppStringId.OCR_PROCESSING_HINT else resume.ocrStatus.stringId(),
                    ),
                )
            }
            if (isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppDesign.colors.brand,
                    trackColor = AppDesign.colors.brandSubtle,
                )
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
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (resume.ocrStatus == OcrStatus.READY) {
                    Button(modifier = Modifier.weight(1f), onClick = onStart) {
                        Text(strings.get(AppStringId.START_SESSION))
                    }
                    if (!ocrText.isNullOrBlank()) {
                        TextButton(onClick = onViewContent) {
                            Text(strings.get(AppStringId.RESUME_VIEW_CONTENT))
                        }
                    }
                }
                if (resume.ocrStatus == OcrStatus.FAILED && !isProcessing) {
                    FilledTonalButton(modifier = Modifier.weight(1f), onClick = onRetry) {
                        Text(strings.get(AppStringId.COMMON_RETRY))
                    }
                }
                TextButton(onClick = onDelete, enabled = !isProcessing) {
                    Text(
                        strings.get(AppStringId.COMMON_DELETE),
                        color = if (isProcessing) {
                            AppDesign.colors.textTertiary
                        } else {
                            AppDesign.colors.danger
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OcrStatusPill(
    status: OcrStatus,
    isProcessing: Boolean,
    label: String,
) {
    val containerColor = when {
        isProcessing -> AppDesign.colors.brandSubtle
        status == OcrStatus.READY -> AppDesign.colors.success.copy(alpha = 0.12f)
        status == OcrStatus.FAILED -> AppDesign.colors.danger.copy(alpha = 0.1f)
        else -> AppDesign.colors.warning.copy(alpha = 0.14f)
    }
    val contentColor = when {
        isProcessing -> AppDesign.colors.brand
        status == OcrStatus.READY -> AppDesign.colors.success
        status == OcrStatus.FAILED -> AppDesign.colors.danger
        else -> AppDesign.colors.warning
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDesign.spacing.sm,
                vertical = AppDesign.spacing.xs,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = contentColor,
                    strokeWidth = 1.5.dp,
                )
            }
            Text(
                text = label,
                style = AppDesign.typography.caption,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResumeContentScreen(
    resume: Resume,
    strings: StringsProvider,
    onNavigateBack: () -> Unit,
    onSaveOcrText: (String) -> Unit,
) {
    val context = LocalContext.current
    val fileStore = remember {
        org.koin.core.context.GlobalContext.get().get<com.example.interviewassistant.core.file.AppFileStore>()
    }
    val privacyRedactor = remember { org.koin.core.context.GlobalContext.get().get<PrivacyRedactor>() }
    val savedContent = resume.ocrText.orEmpty()
    val originalContent = resume.ocrOriginalText.orEmpty().ifBlank { savedContent }
    var draftText by remember(resume.id, savedContent) { mutableStateOf(savedContent) }
    var isEditing by remember(resume.id) { mutableStateOf(false) }
    var showOriginal by remember(resume.id) { mutableStateOf(false) }
    var renderHtml by remember(resume.id) { mutableStateOf(savedContent.looksLikeHtml()) }
    var redactPreview by remember(resume.id) { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var assetBaseUri by remember { mutableStateOf<String?>(null) }
    val isDirty = draftText != savedContent
    val canCompareOriginal = originalContent.isNotBlank() && originalContent != savedContent
    val baseContent = when {
        isEditing -> draftText
        showOriginal -> originalContent
        else -> draftText
    }
    // 脱敏预览仅影响展示与复制；编辑态始终操作可编辑文本
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

    BackHandler { handleBack() }
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
                title = {
                    Text(
                        text = strings.get(AppStringId.RESUME_CONTENT_TITLE),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = strings.get(AppStringId.COMMON_BACK),
                        )
                    }
                },
                actions = {
                    if (isEditing) {
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
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert),
                                    contentDescription = strings.get(AppStringId.RESUME_CONTENT_TITLE),
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                            ) {
                                if (canCompareOriginal || originalContent.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                strings.get(
                                                    if (showOriginal) {
                                                        AppStringId.RESUME_VIEW_CURRENT
                                                    } else {
                                                        AppStringId.RESUME_VIEW_ORIGINAL
                                                    },
                                                ),
                                            )
                                        },
                                        enabled = originalContent.isNotBlank(),
                                        onClick = {
                                            showOriginal = !showOriginal
                                            showMoreMenu = false
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            strings.get(
                                                if (redactPreview) {
                                                    AppStringId.RESUME_REDACT_ORIGINAL
                                                } else {
                                                    AppStringId.RESUME_REDACT_PREVIEW
                                                },
                                            ),
                                        )
                                    },
                                    onClick = {
                                        redactPreview = !redactPreview
                                        showMoreMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            strings.get(
                                                if (renderHtml) {
                                                    AppStringId.RESUME_SHOW_SOURCE
                                                } else {
                                                    AppStringId.RESUME_RENDER_HTML
                                                },
                                            ),
                                        )
                                    },
                                    onClick = {
                                        renderHtml = !renderHtml
                                        showMoreMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            strings.get(
                                                if (copied) AppStringId.RESUME_COPIED else AppStringId.RESUME_COPY,
                                            ),
                                        )
                                    },
                                    onClick = {
                                        copyTextToClipboard(context, displayContent)
                                        copied = true
                                        showMoreMenu = false
                                    },
                                )
                            }
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
                .padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Text(
                text = resume.displayName,
                style = AppDesign.typography.sectionTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (isEditing && originalContent.isNotBlank()) {
                TextButton(
                    onClick = { draftText = originalContent },
                    enabled = draftText != originalContent,
                ) {
                    Text(strings.get(AppStringId.RESUME_RESTORE_ORIGINAL))
                }
            }
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
                    AndroidHtmlPreview(
                        html = OcrDisplayHtml.document(displayContent, assetBaseUri),
                        baseUri = assetBaseUri,
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
        colors = CardDefaults.cardColors(containerColor = AppDesign.colors.surface),
        border = BorderStroke(1.dp, AppDesign.colors.divider),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = AppDesign.colors.brandSubtle,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_assistant),
                        contentDescription = null,
                        modifier = Modifier.padding(AppDesign.spacing.sm).size(22.dp),
                        tint = AppDesign.colors.brand,
                    )
                }
                Text(
                    text = session.title,
                    modifier = Modifier.weight(1f),
                    style = AppDesign.typography.itemTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(modifier = Modifier.weight(1f), onClick = onOpen) {
                    Text(strings.get(AppStringId.OPEN_SESSION))
                }
                TextButton(onClick = onDelete) {
                    Text(
                        text = strings.get(AppStringId.COMMON_DELETE),
                        color = AppDesign.colors.danger,
                    )
                }
            }
        }
    }
}

private data class ImportedDocument(
    val fileName: String,
    val mimeType: String,
    val content: ByteArray,
)

private sealed interface ClipboardResumeResult {
    data class Documents(val documents: List<ImportedDocument>) : ClipboardResumeResult
    data object Unsupported : ClipboardResumeResult
    data object Empty : ClipboardResumeResult
}

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
    val mimeType = contentResolver.getType(uri) ?: mimeTypeForResumeFileName(fileName)
    val content = requireNotNull(contentResolver.openInputStream(uri)) { "Unable to open selected resume" }
        .use { it.readBytes() }
    return ImportedDocument(fileName, mimeType, content)
}

private fun android.content.Context.readClipboardResume(): ClipboardResumeResult {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return ClipboardResumeResult.Empty
    val clip = clipboard.primaryClip ?: return ClipboardResumeResult.Empty
    if (clip.itemCount <= 0) return ClipboardResumeResult.Empty

    val documents = mutableListOf<ImportedDocument>()
    var sawUnsupported = false
    for (index in 0 until clip.itemCount) {
        val item = clip.getItemAt(index)
        val uri = item.uri
        if (uri != null) {
            runCatching { readDocument(uri) }
                .onSuccess { document ->
                    if (document.isSupportedResume()) {
                        documents += document
                    } else {
                        sawUnsupported = true
                    }
                }
        }
    }
    return when {
        documents.isNotEmpty() -> ClipboardResumeResult.Documents(documents)
        sawUnsupported -> ClipboardResumeResult.Unsupported
        else -> ClipboardResumeResult.Empty
    }
}

private fun ClipData.resumeUris(): List<Uri> {
    return buildList {
        for (index in 0 until itemCount) {
            getItemAt(index).uri?.let(::add)
        }
    }
}

private fun android.content.ClipDescription.hasResumeMime(): Boolean {
    return hasMimeType("application/pdf") ||
        hasMimeType("image/*") ||
        hasMimeType("image/jpeg") ||
        hasMimeType("image/png") ||
        hasMimeType("*/*")
}

private fun ImportedDocument.isSupportedResume(): Boolean {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return mimeType in SUPPORTED_RESUME_MIME_TYPES || extension in SUPPORTED_RESUME_EXTENSIONS
}

private fun mimeTypeForResumeFileName(fileName: String): String {
    return when (fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "application/octet-stream"
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
private val SUPPORTED_RESUME_MIME_TYPES = setOf("application/pdf", "image/jpeg", "image/png", "image/jpg")

