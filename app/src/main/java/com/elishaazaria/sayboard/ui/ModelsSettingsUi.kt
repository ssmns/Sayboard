package com.elishaazaria.sayboard.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Divider
import androidx.compose.material.Switch
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.SettingsActivity
import com.elishaazaria.sayboard.Tools
import com.elishaazaria.sayboard.data.ModelLink
import com.elishaazaria.sayboard.data.ModelSizeTier
import com.elishaazaria.sayboard.downloader.FileDownloader
import com.elishaazaria.sayboard.downloader.messages.CancelCurrent
import com.elishaazaria.sayboard.downloader.messages.CancelFinished
import com.elishaazaria.sayboard.downloader.messages.CancelPending
import com.elishaazaria.sayboard.downloader.messages.DownloadError
import com.elishaazaria.sayboard.downloader.messages.DownloadProgress
import com.elishaazaria.sayboard.downloader.messages.DownloadState
import com.elishaazaria.sayboard.downloader.messages.ModelExported
import com.elishaazaria.sayboard.downloader.messages.ModelInfo
import com.elishaazaria.sayboard.downloader.messages.State
import com.elishaazaria.sayboard.downloader.messages.Status
import com.elishaazaria.sayboard.downloader.messages.StatusQuery
import com.elishaazaria.sayboard.downloader.messages.UnzipProgress
import com.elishaazaria.sayboard.recognition.recognizers.providers.Providers
import com.elishaazaria.sayboard.share.ModelShareClient
import com.elishaazaria.sayboard.share.ModelShareService
import com.elishaazaria.sayboard.share.ShareDevice
import com.elishaazaria.sayboard.share.SharedModel
import com.elishaazaria.sayboard.sayboardPreferenceModel
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import java.util.Locale
import kotlin.concurrent.thread

data class DownloadModelProgress(
    val info: ModelInfo, var state: State, var progress: Float
) {
    fun withProgress(newProgress: Float): DownloadModelProgress {
        return DownloadModelProgress(info, state, newProgress)
    }

    fun withState(newState: State): DownloadModelProgress {
        return DownloadModelProgress(info, newState, progress)
    }
}

class ModelsSettingsUi(private val activity: SettingsActivity) {
    private val prefs by sayboardPreferenceModel()

    private val modelsPendingDownloadLD = MutableLiveData<List<ModelInfo>>(mutableListOf())
    private val modelsPendingDownload = mutableListOf<ModelInfo>()

    private val currentDownloadingModel = MutableLiveData<DownloadModelProgress?>()

    private val recognizerSourceProviders = Providers(activity)

    private val shareDevicesLD = MutableLiveData<List<ShareDevice>>(emptyList())
    private val shareScanningLD = MutableLiveData(false)
    private val shareServerRunningLD = MutableLiveData(false)
    private val shareServerAddressLD = MutableLiveData("")

    fun onCreate() {
        reloadModels()
    }

    private fun reloadModels() {
        Log.d(TAG, "Reloading Models")
        val currentModels = prefs.modelsOrder.get().toMutableList()
        val installedModels = recognizerSourceProviders.installedModels()
        currentModels.removeAll { it !in installedModels }
        for (model in installedModels) {
            if (model !in currentModels) {
                currentModels.add(model)
            }
        }
        prefs.modelsOrder.set(currentModels)
    }


    @Composable
    fun Content() {
        val modelsPendingDownload by modelsPendingDownloadLD.observeAsState(mutableListOf())
        val currentDownloadingModel by currentDownloadingModel.observeAsState()
        val modelOrder by prefs.modelsOrder.observeAsState()

        var modelOrderData by remember {
            mutableStateOf(modelOrder)
        }
        modelOrderData = modelOrder

        val state = rememberReorderableLazyListState(onMove = { from, to ->
            // one for currently downloading, one for the separator, and all the pending downloads
            val fromIndex = from.index - modelsPendingDownload.size - 2
            val toIndex = to.index - modelsPendingDownload.size - 2
            if (fromIndex < 0 || toIndex < 0) return@rememberReorderableLazyListState

            prefs.modelsOrder.set(modelOrderData.toMutableList().apply {
                add(
                    toIndex, removeAt(fromIndex)
                )
            })
        })
        Log.d("ModelsSettingsUi", modelOrderData.joinToString())
        LazyColumn(
            state = state.listState,
            modifier = Modifier
                .fillMaxSize()
                .reorderable(state)
                .detectReorderAfterLongPress(state)
        ) {
            item {
                currentDownloadingModel?.let { current ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Card {
                            Column {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = current.info.locale.displayName,
                                            fontSize = 20.sp
                                        )
                                        Text(text = current.info.url, fontSize = 12.sp)
                                        val stateText = stringResource(
                                            id = when (current.state) {
                                                State.NONE -> R.string.models_download_state_unknown
                                                State.QUEUED -> R.string.models_download_state_pending
                                                State.DOWNLOAD_STARTED -> R.string.models_download_state_download_started
                                                State.DOWNLOAD_FINISHED -> R.string.models_download_state_download_finished
                                                State.UNZIP_STARTED -> R.string.models_download_state_unzip_started
                                                State.UNZIP_FINISHED -> R.string.models_download_state_unzip_finished
                                                State.EXPORTING -> R.string.models_download_state_exporting
                                                State.FINISHED -> R.string.models_download_state_finished
                                                State.ERROR -> R.string.models_download_state_error
                                                State.CANCELED -> R.string.models_download_state_canceled
                                            }
                                        )

                                        Text(
                                            text = stringResource(id = R.string.models_download_state).format(
                                                stateText
                                            ), fontSize = 14.sp
                                        )
                                    }
                                    IconButton(onClick = {
                                        EventBus.getDefault().post(
                                            CancelCurrent(current.info)
                                        )
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = null
                                        )
                                    }
                                }
                                LinearProgressIndicator(
                                    current.progress, modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            items(items = modelsPendingDownload) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {

                    Card {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = it.locale.displayName, fontSize = 20.sp)
                                Text(text = it.url, fontSize = 12.sp)
                                Text(
                                    text = stringResource(id = R.string.models_pending_download_state),
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(onClick = {
                                EventBus.getDefault().post(
                                    CancelPending(it)
                                )
                            }) {
                                Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                            }
                        }
                    }
                }
            }

            item {
                if (currentDownloadingModel != null || modelsPendingDownload.isNotEmpty()) {
                    Spacer(
                        modifier = Modifier
                            .padding(10.dp, 5.dp)
                            .background(MaterialTheme.colors.primary)
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }
            }

            items(modelOrderData, { it.path }) { item ->
                ReorderableItem(
                    state, key = item.path, modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) { isDragging ->
                    val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                    Card(
                        modifier = Modifier
                            .shadow(elevation.value)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                .padding(10.dp)
                        ) {

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name
                                        ?: stringResource(id = R.string.models_model_display_name_null),
                                    fontSize = 20.sp
                                )
                                ModelLink.forFilename(item.path.substringAfterLast('/'))?.let { link ->
                                    Text(
                                        text = stringResource(
                                            R.string.models_download_item_details,
                                            formatModelSize(link),
                                            tierLabel(link.sizeTier)
                                        ),
                                        fontSize = 12.sp
                                    )
                                }
                                Text(text = item.path, fontSize = 12.sp)
                            }
                            IconButton(onClick = {
                                FileDownloader.exportModel(
                                    item.path, item.path.substringAfterLast('/'), activity
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(
                                        id = R.string.share_model_file
                                    )
                                )
                            }
                            IconButton(onClick = {
                                Tools.deleteModel(item, activity)
                                reloadModels()
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun Fab() {
        val modelOrder by prefs.modelsOrder.observeAsState()
        var showDownloadDialog by remember {
            mutableStateOf(false)
        }

        var showImportDialog by remember {
            mutableStateOf(false)
        }

        // Models that are bigger than the "small" ones need a confirmation,
        // since they can be several gigabytes and need a lot of RAM.
        var modelPendingConfirmation by remember {
            mutableStateOf<ModelLink?>(null)
        }

        var showShareDialog by remember {
            mutableStateOf(false)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FloatingActionButton(onClick = {
                showDownloadDialog = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        activity.applicationContext, Manifest.permission.POST_NOTIFICATIONS
                    )
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                            activity, arrayOf(
                                Manifest.permission.POST_NOTIFICATIONS
                            ), SettingsActivity.PERMISSION_REQUEST_POST_NOTIFICATIONS
                        )
                    }
                }
            }) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
            }

            FloatingActionButton(onClick = {
                showImportDialog = true
            }) {
                Icon(imageVector = Icons.Default.Folder, contentDescription = null)
            }

            FloatingActionButton(onClick = {
                refreshShareState()
                showShareDialog = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        activity.applicationContext, Manifest.permission.POST_NOTIFICATIONS
                    )
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                            activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            SettingsActivity.PERMISSION_REQUEST_POST_NOTIFICATIONS
                        )
                    }
                }
            }) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
            }
        }

        if (showShareDialog) {
            ShareDialog(onDismiss = { showShareDialog = false })
        }

        if (showDownloadDialog) {
            val installedFolders = modelOrder.map { it.path.substringAfterLast('/') }.toSet()
            val modelsByLanguage = ModelLink.values()
                .groupBy { it.locale }
                .toList()
                .sortedBy { it.first.displayName }

            JetPrefAlertDialog(
                title = stringResource(id = R.string.models_download_dialog_title),
                onDismiss = { showDownloadDialog = false },
                dismissLabel = stringResource(id = R.string.button_cancel),
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(0.dp, (LocalConfiguration.current.screenHeightDp * 0.9).dp)
                ) {
                    item(key = "hint") {
                        Text(
                            text = stringResource(id = R.string.models_download_dialog_hint),
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    modelsByLanguage.forEach { (locale, models) ->
                        item(key = "lang-${locale.toLanguageTag()}") {
                            Text(
                                text = locale.displayName,
                                fontSize = 16.sp,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }
                        items(items = models, key = { it.name }) { ml ->
                            val installed = ml.folderName in installedFolders
                            Card(
                                onClick = {
                                    when {
                                        installed -> Toast.makeText(
                                            activity,
                                            activity.getString(
                                                R.string.models_already_installed, ml.filename
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        ml.sizeTier == ModelSizeTier.SMALL -> {
                                            showDownloadDialog = false
                                            FileDownloader.downloadModel(ml, activity)
                                        }

                                        else -> modelPendingConfirmation = ml
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colors.onSurface.copy(0.2f))
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .background(MaterialTheme.colors.onSurface.copy(0.2f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = ml.filename,
                                        fontSize = 15.sp,
                                        color = if (installed) {
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colors.onSurface
                                        }
                                    )
                                    Text(
                                        text = if (installed) {
                                            stringResource(
                                                R.string.models_download_item_installed,
                                                formatModelSize(ml),
                                                tierLabel(ml.sizeTier)
                                            )
                                        } else {
                                            stringResource(
                                                R.string.models_download_item_details,
                                                formatModelSize(ml),
                                                tierLabel(ml.sizeTier)
                                            )
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        modelPendingConfirmation?.let { model ->
            DownloadConfirmationDialog(
                model = model,
                onDismiss = { modelPendingConfirmation = null },
                onConfirm = {
                    modelPendingConfirmation = null
                    showDownloadDialog = false
                    FileDownloader.downloadModel(model, activity)
                }
            )
        }

        if (showImportDialog) {
            AlertDialog(onDismissRequest = {

            }, confirmButton = {
                Button(onClick = {
                    showImportDialog = false
                    activity.importModel()
                }) {
                    Text(text = stringResource(id = R.string.models_import_dialog_import))
                }
            }, dismissButton = {
                Button(onClick = { showImportDialog = false }) {
                    Text(text = stringResource(id = R.string.button_cancel))
                }
            }, title = {
                Text(text = stringResource(id = R.string.models_import_dialog_title))
            }, text = {
                val annotatedString = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onSurface)) {
                        append(stringResource(id = R.string.models_import_dialog_text_before_link))
                    }

                    pushStringAnnotation(
                        tag = "vosk-website", annotation = "https://alphacephei.com/vosk/models"
                    )
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                        append(stringResource(id = R.string.models_import_dialog_text_link))
                    }
                    pop()

                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onSurface)) {
                        append(stringResource(id = R.string.models_import_dialog_text_after_link))
                    }
                }

                ClickableText(text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = "vosk-website", start = offset, end = offset
                        ).firstOrNull()?.let {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item))
                            activity.startActivity(browserIntent)
                        }
                    })
            })
        }
    }

    /**
     * Shown before downloading a model that is bigger than the lightweight ones,
     * so the user knows what they are getting into.
     */
    @Composable
    private fun DownloadConfirmationDialog(
        model: ModelLink,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        val requiredStorageMb = Tools.getRequiredStorageMb(model.sizeMb.toLong())
        val availableStorageMb = remember(model) { Tools.getAvailableModelsStorageMb(activity) }
        val hasEnoughStorage = availableStorageMb < 0 || availableStorageMb >= requiredStorageMb

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(id = R.string.models_download_confirm_title))
            },
            text = {
                Column {
                    Text(
                        text = model.filename,
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.models_download_confirm_size,
                            formatModelSize(model),
                            tierLabel(model.sizeTier)
                        ),
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(
                            R.string.models_download_confirm_ram,
                            model.sizeTier.recommendedRamMb
                        ),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = model.notes, fontSize = 13.sp)
                    Text(
                        text = stringResource(
                            R.string.models_download_confirm_license, model.license
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (hasEnoughStorage) {
                        Text(
                            text = stringResource(
                                R.string.models_download_confirm_space_ok,
                                availableStorageMb,
                                requiredStorageMb
                            ),
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = stringResource(
                                R.string.models_download_confirm_space_not_enough,
                                availableStorageMb,
                                requiredStorageMb
                            ),
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = onConfirm, enabled = hasEnoughStorage) {
                    Text(text = stringResource(id = R.string.models_download_confirm_download))
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.button_cancel))
                }
            }
        )
    }

    @Composable
    private fun tierLabel(tier: ModelSizeTier): String = stringResource(
        id = when (tier) {
            ModelSizeTier.SMALL -> R.string.models_size_tier_small
            ModelSizeTier.MEDIUM -> R.string.models_size_tier_medium
            ModelSizeTier.LARGE -> R.string.models_size_tier_large
            ModelSizeTier.HUGE -> R.string.models_size_tier_huge
        }
    )

    private fun formatModelSize(model: ModelLink): String {
        return if (model.sizeMb >= 1000) {
            String.format(Locale.getDefault(), "%.1f GB", model.sizeMb / 1000f)
        } else {
            String.format(Locale.getDefault(), "%d MB", model.sizeMb)
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun ShareDialog(onDismiss: () -> Unit) {
        val devices by shareDevicesLD.observeAsState(emptyList())
        val scanning by shareScanningLD.observeAsState(false)
        val serverRunning by shareServerRunningLD.observeAsState(false)
        val serverAddress by shareServerAddressLD.observeAsState("")
        val modelOrder by prefs.modelsOrder.observeAsState()
        val installedNames = modelOrder.map { it.path.substringAfterLast('/') }.toSet()
        var manualAddress by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            refreshShareState()
        }

        JetPrefAlertDialog(
            title = stringResource(id = R.string.share_dialog_title),
            onDismiss = onDismiss,
            dismissLabel = stringResource(id = R.string.button_close),
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(0.dp, (LocalConfiguration.current.screenHeightDp * 0.85).dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.share_server_toggle),
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(id = R.string.share_server_toggle_summary),
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = serverRunning,
                        onCheckedChange = { enabled -> setShareServerEnabled(enabled) }
                    )
                }
                if (serverRunning) {
                    Text(
                        text = stringResource(id = R.string.share_server_address, serverAddress),
                        fontSize = 13.sp,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (ModelShareService.lastError != null) {
                    Text(
                        text = stringResource(
                            id = R.string.share_server_error, ModelShareService.lastError ?: ""
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.error
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                Text(
                    text = stringResource(id = R.string.share_find_devices),
                    fontSize = 16.sp
                )
                Text(
                    text = stringResource(id = R.string.share_find_devices_summary),
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Button(
                    onClick = { scanShareDevices() },
                    enabled = !scanning,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = if (scanning) R.string.share_scanning else R.string.share_scan
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    OutlinedTextField(
                        value = manualAddress,
                        onValueChange = { manualAddress = it },
                        singleLine = true,
                        label = {
                            Text(
                                text = stringResource(id = R.string.share_manual_address_hint),
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { connectToShareDevice(manualAddress) },
                        enabled = manualAddress.isNotBlank(),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(text = stringResource(id = R.string.share_connect))
                    }
                }

                if (devices.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(0.dp, 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        devices.forEach { device ->
                            item(key = "device-${device.address}:${device.httpPort}") {
                                Text(
                                    text = stringResource(
                                        id = R.string.share_device_title,
                                        device.name,
                                        device.displayAddress
                                    ),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colors.primary,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }
                            items(device.models, key = { "m-${device.address}-${it.name}" }) { model ->
                                val installed = model.name in installedNames
                                Card(
                                    onClick = {
                                        if (installed) {
                                            Toast.makeText(
                                                activity,
                                                activity.getString(
                                                    R.string.models_already_installed, model.name
                                                ),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            onDismiss()
                                            FileDownloader.downloadSharedModel(
                                                model.downloadUrl(device),
                                                model.name,
                                                model.locale,
                                                activity
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = model.name, fontSize = 14.sp)
                                        Text(
                                            text = stringResource(
                                                if (installed) {
                                                    R.string.models_download_item_installed
                                                } else {
                                                    R.string.models_download_item_details
                                                },
                                                model.sizeText,
                                                model.locale.displayName
                                            ),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(id = R.string.share_offline_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }

    private fun setShareServerEnabled(enabled: Boolean) {
        val intent = Intent(activity, ModelShareService::class.java)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionCheck = ContextCompat.checkSelfPermission(
                    activity.applicationContext, Manifest.permission.POST_NOTIFICATIONS
                )
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        SettingsActivity.PERMISSION_REQUEST_POST_NOTIFICATIONS
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent)
            } else {
                activity.startService(intent)
            }
        } else {
            activity.stopService(intent)
        }
        Handler(Looper.getMainLooper()).postDelayed({ refreshShareState() }, 600)
    }

    private fun refreshShareState() {
        shareServerRunningLD.postValue(ModelShareService.isRunning)
        shareServerAddressLD.postValue(ModelShareService.addressText())
    }

    private fun connectToShareDevice(address: String) {
        val parsed = ModelShareClient.parseAddress(address)
        if (parsed == null) {
            Toast.makeText(activity, R.string.share_bad_address, Toast.LENGTH_SHORT).show()
            return
        }
        shareScanningLD.postValue(true)
        thread(name = "sayboard-share-connect") {
            try {
                val device = ModelShareClient.fetchDevice(parsed.first, parsed.second)
                val existing = shareDevicesLD.value.orEmpty()
                    .filterNot { it.address == device.address && it.httpPort == device.httpPort }
                shareDevicesLD.postValue(existing + device)
            } catch (e: Exception) {
                Log.w(TAG, "Could not connect to $address", e)
                activity.runOnUiThread {
                    Toast.makeText(activity, R.string.share_no_devices, Toast.LENGTH_SHORT).show()
                }
            } finally {
                shareScanningLD.postValue(false)
            }
        }
    }

    private fun scanShareDevices() {
        shareScanningLD.postValue(true)
        thread(name = "sayboard-share-scan") {
            val found = try {
                ModelShareClient.discover(SCAN_TIMEOUT_MS)
            } catch (e: Exception) {
                Log.w(TAG, "Discovery failed", e)
                emptyList()
            }
            shareDevicesLD.postValue(found)
            shareScanningLD.postValue(false)
            if (found.isEmpty()) {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity, R.string.share_no_devices, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateCurrentDownloading(info: ModelInfo): DownloadModelProgress {
        var currentModel = currentDownloadingModel.value

        if (currentModel == null) {
            currentModel = DownloadModelProgress(
                info, State.NONE, 0f
            )
        } else if (currentModel.info != info) {
            val newCurrent = modelsPendingDownload.find { it == currentModel!!.info }
            if (newCurrent == null) {
                currentModel = DownloadModelProgress(
                    info, State.NONE, 0f
                )
            } else {
                modelsPendingDownload.remove(newCurrent)
                modelsPendingDownloadLD.postValue(modelsPendingDownload.toList())
                currentModel = DownloadModelProgress(
                    newCurrent, State.NONE, 0f
                )
            }
        }
        currentDownloadingModel.postValue(currentModel)
        return currentModel
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onAddToDownload(info: ModelInfo) {
        Log.d(TAG, "onAddToDownload($info)")

        if (modelsPendingDownload.contains(info)) return
        modelsPendingDownload.add(info)
        modelsPendingDownloadLD.postValue(modelsPendingDownload.toList())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onState(state: DownloadState) {
        Log.d(TAG, "onState($state)")
        when (state.state) {
            State.QUEUED -> onAddToDownload(state.info)
            State.DOWNLOAD_STARTED, State.NONE -> {
                modelsPendingDownload.remove(state.info)
                modelsPendingDownloadLD.postValue(modelsPendingDownload.toList())
                currentDownloadingModel.postValue(
                    updateCurrentDownloading(state.info).withState(
                        state.state
                    )
                )
            }

            State.FINISHED -> {
                currentDownloadingModel.postValue(null)
                reloadModels()
            }

            else -> {
                currentDownloadingModel.postValue(
                    updateCurrentDownloading(state.info).withState(
                        state.state
                    )
                )
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onStatus(status: Status) {
        Log.d(TAG, "onStatus($status)")
        if (status.current != null) {
            onState(DownloadState(status.current, status.state))
            when (status.state) {
                State.DOWNLOAD_STARTED -> onDownloadProgress(
                    DownloadProgress(
                        status.current, status.downloadProgress
                    )
                )

                State.UNZIP_STARTED -> onUnzipProgress(
                    UnzipProgress(
                        status.current, status.unzipProgress
                    )
                )

                else -> {}
            }
        } else {
            currentDownloadingModel.postValue(null)
        }
        modelsPendingDownload.clear()
        for (modelInfo in status.queued) {
            onState(DownloadState(modelInfo, State.QUEUED))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onDownloadProgress(progress: DownloadProgress) {
//        Log.d(TAG, "onDownloadProgress($progress)")
        // CG wasteful, but works
        currentDownloadingModel.postValue(
            updateCurrentDownloading(progress.info).withProgress(
                progress.progress
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onUnzipProgress(progress: UnzipProgress) {
//        Log.d(TAG, "onUnzipProgress($progress)")
        currentDownloadingModel.postValue(
            updateCurrentDownloading(progress.info).withProgress(
                progress.progress
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onCancelFinished(event: CancelFinished) {
        if (currentDownloadingModel.value?.info == event.info) {
            currentDownloadingModel.postValue(null)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onModelExported(event: ModelExported) {
        Toast.makeText(
            activity,
            activity.getString(R.string.share_export_ready, event.info.filename),
            Toast.LENGTH_SHORT
        ).show()
        // Offer the share sheet right away when the user is looking at the app.
        if (!activity.isFinishing && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            Tools.shareModelZip(activity, event.zipFile, event.info.filename)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDownloadError(error: DownloadError) {
        Toast.makeText(activity, error.message, Toast.LENGTH_SHORT).show()
    }

    fun onStart() {
        EventBus.getDefault().register(this)
        EventBus.getDefault().post(StatusQuery())
    }

    fun onStop() {
        EventBus.getDefault().unregister(this)
    }

    fun onResume() {
        reloadModels()
        EventBus.getDefault().post(StatusQuery())
    }

    companion object {
        private const val TAG = "ModelsSettingUi"
        private const val SCAN_TIMEOUT_MS = 2500L
    }
}