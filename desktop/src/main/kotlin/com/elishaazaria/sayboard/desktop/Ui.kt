package com.elishaazaria.sayboard.desktop

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser

/** The Persian/Latin font that ships inside the app, so no font is missing. */
private val AppFontFamily = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.platform.Font("fonts/Vazirmatn-Regular.ttf", FontWeight.Normal),
    androidx.compose.ui.text.platform.Font("fonts/Vazirmatn-Medium.ttf", FontWeight.Medium),
    androidx.compose.ui.text.platform.Font("fonts/Vazirmatn-Bold.ttf", FontWeight.Bold),
)

private val AppTypography = androidx.compose.material.Typography(defaultFontFamily = AppFontFamily)

private val Primary = Color(0xFF3F51B5)
private val PrimaryDark = Color(0xFF2C3A8C)
private val Muted = Color(0xFF767C8A)
private val Background = Color(0xFFF4F6FA)
private val CardBorder = Color(0xFFE2E6EF)
private val OkGreen = Color(0xFF1B7F3B)
private val WarnAmber = Color(0xFFB26A00)
private val CardShape = RoundedCornerShape(12.dp)

internal val LocalStrings = compositionLocalOf { Strings.english }

/** Technical values (versions, paths, commands) always read left to right. */
@Composable
internal fun LtrText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            lineHeight = lineHeight,
        )
    }
}

@Composable
fun AppScreen(controller: AppController) {
    val s = controller.strings
    val direction = if (s.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalStrings provides s, LocalLayoutDirection provides direction) {
        MaterialTheme(
            typography = AppTypography,
            colors = lightColors(
                primary = Primary,
                primaryVariant = PrimaryDark,
                secondary = OkGreen,
                background = Background,
                surface = Color.White,
            )
        ) {
            var tab by remember { mutableStateOf(0) }
            var pendingDownload by remember { mutableStateOf<CatalogModel?>(null) }
            var pendingFolder by remember { mutableStateOf<File?>(null) }

            Surface(Modifier.fillMaxSize(), color = Background) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Header(controller)
                        PreviewBar(controller)
                        RequirementsBanner(controller)
                        Spacer(Modifier.height(12.dp))
                        TabRow(
                            selectedTabIndex = tab,
                            backgroundColor = Color.Transparent,
                            contentColor = Primary,
                            modifier = Modifier.width(440.dp),
                        ) {
                            Tab(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                text = { Text(s["tab.models"]) },
                            )
                            Tab(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                text = { Text(s["tab.settings"]) },
                            )
                            Tab(
                                selected = tab == 2,
                                onClick = { tab = 2 },
                                text = { Text(s["tab.about"]) },
                            )
                        }
                        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 14.dp)) {
                            when (tab) {
                                0 -> ModelsTab(controller) { pendingDownload = it }
                                1 -> SettingsTab(controller) { pendingFolder = it }
                                else -> AboutTab(controller)
                            }
                        }
                        DownloadBar(controller)
                        LogPane(controller)
                    }
                    if (controller.diagnosticsVisible) {
                        RequirementsPanel(controller)
                    }
                }
            }

            pendingDownload?.let { model ->
                DownloadConfirmDialog(model, controller) { pendingDownload = null }
            }
            pendingFolder?.let { folder ->
                MoveModelsDialog(controller, folder) { pendingFolder = null }
            }
        }
    }
}

// ------------------------------------------------------------------------ header

@Composable
private fun Header(controller: AppController) {
    val s = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource("icons/saytypist-mark.png"),
                    contentDescription = null,
                    modifier = Modifier.width(32.dp).height(32.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    AppInfo.APP_NAME,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E2433),
                )
                Spacer(Modifier.width(10.dp))
                StatusChip(controller.status)
            }
            Spacer(Modifier.height(2.dp))
            Text(s["app.subtitle"], fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(4.dp))
            Text(
                s.f("header.hotkey", controller.config.hotkey) + "   ·   " +
                        s.f("header.typing", controller.typingMethod),
                fontSize = 12.sp,
                color = Muted,
            )
        }
        LanguagePicker(controller)
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { controller.toggleLog() }) {
            Text(
                if (controller.config.logVisible) s["header.hideLog"] else s["header.showLog"],
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = { controller.toggleDictation() },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(44.dp),
        ) {
            Text(
                if (controller.status == RecognitionState.LISTENING ||
                    controller.status == RecognitionState.LOADING
                ) {
                    s["action.stop"]
                } else {
                    s["action.start"]
                }
            )
        }
    }
}

@Composable
private fun StatusChip(state: RecognitionState) {
    val s = LocalStrings.current
    val (label, color) = when (state) {
        RecognitionState.IDLE -> s["status.idle"] to Muted
        RecognitionState.LOADING -> s["status.loading"] to WarnAmber
        RecognitionState.LISTENING -> s["status.listening"] to OkGreen
        RecognitionState.ERROR -> s["status.error"] to MaterialTheme.colors.error
    }
    Box(
        Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LanguagePicker(controller: AppController) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(36.dp),
        ) {
            Text("${s.lang.label}  ▾", fontSize = 12.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Lang.values().forEach { lang ->
                DropdownMenuItem(onClick = {
                    expanded = false
                    controller.setLanguage(lang)
                }) {
                    Text(lang.label, fontSize = 13.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------------------------- preview

@Composable
private fun PreviewBar(controller: AppController) {
    val s = LocalStrings.current
    if (!controller.config.showPreview) return
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = controller.preview.ifBlank { s["preview.hint"] },
            color = if (controller.preview.isBlank()) Muted else OkGreen,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { controller.copyLastPhrase() },
            enabled = controller.lastPhrase.isNotBlank() || controller.preview.isNotBlank(),
        ) { Text(s["action.copyLast"], fontSize = 12.sp) }
    }
}

// ------------------------------------------------------------------------ banner

@Composable
private fun RequirementsBanner(controller: AppController) {
    val s = LocalStrings.current
    val problems = controller.diagnostics.filter { it.state != CheckState.OK }
    if (problems.isEmpty()) return
    val hasError = problems.any { it.state == CheckState.ERROR }
    val color = if (hasError) Color(0xFFFDECEC) else Color(0xFFFFF7E6)
    val accent = if (hasError) MaterialTheme.colors.error else WarnAmber
    Spacer(Modifier.height(10.dp))
    Surface(color = color, shape = CardShape, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(if (hasError) "✖" else "⚠", fontSize = 16.sp, color = accent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(problems.first().title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(problems.first().detail, fontSize = 12.sp, color = Color(0xFF4A4F5C))
            }
            TextButton(onClick = { controller.openDiagnostics() }) {
                Text(s["banner.install"], fontSize = 12.sp)
            }
        }
    }
}

// ------------------------------------------------------------------------ models

@Composable
private fun ModelsTab(controller: AppController, onLargeDownload: (CatalogModel) -> Unit) {
    val s = LocalStrings.current
    var search by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    val installedNames = controller.installed.map { it.name }.toSet()
    val filtered = remember(search, language) {
        ModelCatalog.models.filter { model ->
            (language.isBlank() || model.lang == language) &&
                    (search.isBlank() ||
                            model.id.contains(search, true) ||
                            model.lang.contains(search, true) ||
                            model.notes.contains(search, true))
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.weight(0.42f).fillMaxHeight(),
            shape = CardShape,
            elevation = 0.dp,
            backgroundColor = Color.White,
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        s["models.installed"],
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${controller.installed.size} · ${controller.totalModelsSize()}",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { controller.refreshInstalled() }) {
                        Text(s["action.refresh"], fontSize = 12.sp)
                    }
                    TextButton(onClick = { pickZipFile()?.let { controller.importZip(it) } }) {
                        Text(s["action.importZip"], fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (controller.installed.isEmpty()) {
                    Text(s["models.none"], fontSize = 13.sp, color = Muted)
                } else {
                    val listState = rememberLazyListState()
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(controller.installed, key = { it.name }) { model ->
                                InstalledModelCard(controller, model)
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(listState),
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.weight(0.58f).fillMaxHeight(),
            shape = CardShape,
            elevation = 0.dp,
            backgroundColor = Color.White,
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        s["models.catalog"],
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${filtered.size}",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                }
                Spacer(Modifier.height(10.dp))
                // both controls share the same height so the row looks even
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text(s["models.search"], fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(FILTER_HEIGHT),
                    )
                    Spacer(Modifier.width(10.dp))
                    FilterDropdown(
                        selected = if (language.isBlank()) s["models.allLanguages"] else language,
                        options = listOf(s["models.allLanguages"] to "") +
                                ModelCatalog.languages.map { it to it },
                        modifier = Modifier.width(190.dp).height(FILTER_HEIGHT),
                    ) { language = it }
                }
                Spacer(Modifier.height(8.dp))
                Text(s["models.hint"], fontSize = 11.sp, color = Muted)
                Spacer(Modifier.height(8.dp))
                val listState = rememberLazyListState()
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                    ) {
                        items(filtered, key = { it.id }) { model ->
                            AvailableModelRow(
                                model = model,
                                installed = model.id in installedNames,
                                onDownload = {
                                    if (model.isHeavy) {
                                        onLargeDownload(model)
                                    } else {
                                        controller.downloadModel(model)
                                    }
                                },
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(listState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

private val FILTER_HEIGHT = 54.dp

@Composable
private fun FilterDropdown(
    selected: String,
    options: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selected, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("▾", fontSize = 12.sp, color = Muted)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, value) ->
                DropdownMenuItem(onClick = {
                    expanded = false
                    onSelect(value)
                }) {
                    Text(label, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun InstalledModelCard(controller: AppController, model: InstalledModel) {
    val s = LocalStrings.current
    var confirmDelete by remember { mutableStateOf(false) }
    val active = controller.config.model == model.name
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                if (active) Primary.copy(alpha = 0.06f) else Color(0xFFFAFBFD),
                RoundedCornerShape(10.dp),
            )
            .border(
                1.dp,
                if (active) Primary.copy(alpha = 0.45f) else CardBorder,
                RoundedCornerShape(10.dp),
            )
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    model.name,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LtrText(
                        "${model.lang}  ·  ${model.sizeText}",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                    if (active) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            s["models.activeTag"],
                            fontSize = 11.sp,
                            color = Primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            TextButton(
                onClick = { controller.setActiveModel(model.name) },
                enabled = !active,
            ) { Text(s["action.use"], fontSize = 12.sp) }
            TextButton(onClick = { controller.exportZip(model) }) {
                Text(s["action.exportZip"], fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { confirmDelete = true }) {
                Text(s["action.delete"], fontSize = 12.sp, color = MaterialTheme.colors.error)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(s["models.deleteTitle"]) },
            text = { Text(s.f("models.deleteText", model.name, model.sizeText)) },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = false
                    controller.deleteModel(model.name)
                }) { Text(s["action.delete"]) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(s["action.cancel"]) }
            },
        )
    }
}

@Composable
private fun AvailableModelRow(model: CatalogModel, installed: Boolean, onDownload: () -> Unit) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(Color(0xFFFAFBFD), RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            LtrText(model.id, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            LtrText(
                "${model.sizeText}  ·  ${s["tier." + model.tier.id]}  ·  ${model.license}",
                fontSize = 11.sp,
                color = Muted,
            )
            LtrText(model.notes, fontSize = 11.sp, color = Muted.copy(alpha = 0.85f))
        }
        if (installed) {
            Text(
                s["models.installedTag"],
                fontSize = 11.sp,
                color = Primary,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        } else {
            OutlinedButton(
                onClick = onDownload,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(32.dp),
            ) { Text(s["action.download"], fontSize = 12.sp) }
        }
    }
}

// ---------------------------------------------------------------------- settings

@Composable
private fun SettingsTab(controller: AppController, onPickFolder: (File) -> Unit) {
    val s = LocalStrings.current
    val config = controller.config
    var hotkey by remember(config.hotkey) { mutableStateOf(config.hotkey) }
    var device by remember(config.device) {
        mutableStateOf(config.device.ifBlank { s["settings.defaultMic"] })
    }
    var backend by remember(config.typingBackend) { mutableStateOf(config.typingBackend) }
    var livePartial by remember(config.livePartial) { mutableStateOf(config.livePartial) }
    var trailingSpace by remember(config.trailingSpace) { mutableStateOf(config.trailingSpace) }
    var capitalize by remember(config.capitalize) { mutableStateOf(config.capitalize) }
    var keepModel by remember(config.keepModelLoaded) { mutableStateOf(config.keepModelLoaded) }
    var showPreview by remember(config.showPreview) { mutableStateOf(config.showPreview) }
    var copyToClipboard by remember(config.copyToClipboard) {
        mutableStateOf(config.copyToClipboard)
    }
    var startMinimized by remember(config.startMinimized) {
        mutableStateOf(config.startMinimized)
    }
    var error by remember { mutableStateOf<String?>(null) }
    val devices = remember { RecognitionEngine.inputDevices().map { it.name } }

    fun save(validateHotkey: Boolean = true) {
        error = null
        try {
            controller.applySettings(
                config.copy(
                    hotkey = if (validateHotkey) hotkey.trim() else config.hotkey,
                    device = if (device == s["settings.defaultMic"]) "" else device,
                    typingBackend = backend,
                    livePartial = livePartial,
                    trailingSpace = trailingSpace,
                    capitalize = capitalize,
                    keepModelLoaded = keepModel,
                    showPreview = showPreview,
                    copyToClipboard = copyToClipboard,
                    startMinimized = startMinimized,
                )
            )
        } catch (e: Exception) {
            error = e.message ?: "…"
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(s["settings.typing"]) {
            SettingRow(s["settings.hotkey"]) {
                OutlinedTextField(
                    value = hotkey,
                    onValueChange = { hotkey = it },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(210.dp).height(FILTER_HEIGHT),
                )
                Spacer(Modifier.width(10.dp))
                Button(onClick = { save() }) { Text(s["action.saveHotkey"], fontSize = 12.sp) }
                Spacer(Modifier.width(10.dp))
                LtrText(s["settings.hotkeyHint"], fontSize = 11.sp, color = Muted)
            }
            SettingRow(s["settings.microphone"]) {
                FilterDropdown(
                    selected = device,
                    options = devices.map { it to it },
                    modifier = Modifier.width(320.dp).height(FILTER_HEIGHT),
                ) {
                    device = it
                    save(validateHotkey = false)
                }
            }
            SettingRow(s["settings.typingMethod"]) {
                FilterDropdown(
                    selected = backend,
                    options = Typers.available().map { it to it },
                    modifier = Modifier.width(190.dp).height(FILTER_HEIGHT),
                ) {
                    backend = it
                    save(validateHotkey = false)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { controller.testTyping() }) {
                    Text(s["action.testTyping"], fontSize = 12.sp)
                }
            }
        }

        SectionCard(s["settings.behaviour"]) {
            CheckRow(s["settings.typeLive"], livePartial) {
                livePartial = it
                save(validateHotkey = false)
            }
            CheckRow(s["settings.trailingSpace"], trailingSpace) {
                trailingSpace = it
                save(validateHotkey = false)
            }
            CheckRow(s["settings.capitalize"], capitalize) {
                capitalize = it
                save(validateHotkey = false)
            }
            CheckRow(s["settings.keepModel"], keepModel) {
                keepModel = it
                save(validateHotkey = false)
            }
            CheckRow(s["settings.showPreview"], showPreview) {
                showPreview = it
                save(validateHotkey = false)
            }
            CheckRow(s["settings.copyPhrases"], copyToClipboard) {
                copyToClipboard = it
                save(validateHotkey = false)
            }
            CheckRow(s["settings.startMinimized"], startMinimized) {
                startMinimized = it
                save(validateHotkey = false)
            }
        }

        SectionCard(s["settings.storage"]) {
            Text(s["settings.storageText"], fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(6.dp))
            SelectionContainer {
                Text(
                    controller.modelsFolder().absolutePath,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF33475B),
                )
            }
            Text(
                s.f("settings.freeSpace", formatSizeMb(AppPaths.freeSpaceMb())),
                fontSize = 11.sp,
                color = Muted,
            )
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { pickDirectory()?.let(onPickFolder) }) {
                    Text(s["action.changeFolder"], fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { openFolder(controller.modelsFolder()) }) {
                    Text(s["action.openFolder"], fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { controller.unloadModel() }) {
                    Text(s["settings.unload"], fontSize = 12.sp)
                }
            }
            Divider(Modifier.padding(vertical = 8.dp))
            Text(s.f("settings.settingsFile", AppPaths.configFile().absolutePath), fontSize = 11.sp, color = Muted)
        }

        SectionCard(s["settings.requirements"]) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { controller.refreshDiagnostics() }) {
                    Text(s["action.checkAgain"], fontSize = 12.sp)
                }
                TextButton(onClick = { controller.openDiagnostics() }) {
                    Text(s["action.showDetails"], fontSize = 12.sp)
                }
                Text(s["settings.immediate"], fontSize = 11.sp, color = Muted)
            }
            controller.diagnostics.forEach { check ->
                Row(Modifier.padding(vertical = 1.dp)) {
                    Text(
                        text = when (check.state) {
                            CheckState.OK -> "✔"
                            CheckState.WARNING -> "⚠"
                            CheckState.ERROR -> "✖"
                        },
                        fontSize = 12.sp,
                        color = when (check.state) {
                            CheckState.OK -> OkGreen
                            CheckState.WARNING -> WarnAmber
                            CheckState.ERROR -> MaterialTheme.colors.error
                        },
                        modifier = Modifier.width(20.dp),
                    )
                    Text("${check.title}: ${check.detail}", fontSize = 12.sp)
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colors.error, fontSize = 13.sp) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = CardShape,
        elevation = 0.dp,
        backgroundColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2433))
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, modifier = Modifier.width(150.dp), color = Color(0xFF3A4050))
        content()
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 13.sp)
    }
}

// ------------------------------------------------------------------------- about

@Composable
private fun AboutTab(controller: AppController) {
    val s = LocalStrings.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(s["about.title"]) {
            Image(
                painter = painterResource("icons/saytypist.png"),
                contentDescription = null,
                modifier = Modifier.width(190.dp).height(120.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                s.f("about.version", AppInfo.VERSION),
                fontSize = 12.sp,
                color = Muted,
            )
            Spacer(Modifier.height(10.dp))
            Text(s["about.description"], fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(10.dp))
            Text(s["about.howItWorks"], fontSize = 13.sp, lineHeight = 20.sp)
        }
        SectionCard(s["about.developer"]) {
            Text(AppInfo.MAINTAINER, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (AppInfo.MAINTAINER_URL.isNotBlank()) {
                Text(AppInfo.MAINTAINER_URL, fontSize = 12.sp, color = Primary)
            }
            Spacer(Modifier.height(6.dp))
            Text(s["about.upstream"], fontSize = 12.sp, color = Muted)
            Text(s.f("about.license", AppInfo.LICENSE), fontSize = 12.sp, color = Muted)
        }
        SectionCard(s["about.credits"]) {
            Text(s["about.creditsText"], fontSize = 12.sp, lineHeight = 18.sp)
        }
        SectionCard(s["about.data"]) {
            SelectionContainer {
                Text(
                    s.f(
                        "about.dataText",
                        controller.modelsFolder().absolutePath,
                        AppPaths.configFile().absolutePath,
                    ),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        SectionCard(s["about.links"]) {
            LinkRow(s["about.linkProject"], AppInfo.PROJECT_URL)
            LinkRow(s["about.linkIssues"], AppInfo.ISSUES_URL)
            LinkRow(s["about.linkUpstream"], AppInfo.UPSTREAM)
            LinkRow(s["about.linkVosk"], AppInfo.VOSK_SITE)
            LinkRow(s["about.linkModels"], AppInfo.MODELS_SITE)
            LinkRow(s["about.linkLicense"], AppInfo.LICENSE_URL)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", fontSize = 12.sp)
        SelectionContainer {
            Text(url, fontSize = 12.sp, color = Primary)
        }
    }
}

// -------------------------------------------------------------------- dialogues

@Composable
private fun MoveModelsDialog(controller: AppController, folder: File, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val models = controller.installed
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s["confirm.moveTitle"]) },
        text = {
            Text(
                s.f(
                    "confirm.moveText",
                    models.size,
                    controller.modelsFolder().absolutePath,
                    folder.absolutePath,
                ),
                fontSize = 13.sp,
            )
        },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                controller.changeModelsFolder(folder, moveExisting = true)
            }) { Text(s["action.moveAndSwitch"], fontSize = 12.sp) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onDismiss()
                    controller.changeModelsFolder(folder, moveExisting = false)
                }) { Text(s["action.switchOnly"], fontSize = 12.sp) }
                TextButton(onClick = onDismiss) { Text(s["action.cancel"], fontSize = 12.sp) }
            }
        },
    )
}

@Composable
private fun DownloadConfirmDialog(
    model: CatalogModel,
    controller: AppController,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val freeMb = remember(model) { AppPaths.freeSpaceMb() }
    val requiredMb = controller.requiredSpaceMb(model)
    val enough = freeMb < 0 || freeMb >= requiredMb
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (model.isHeavy) s["confirm.titleHeavy"] else s["confirm.title"])
        },
        text = {
            Column {
                Text(model.id, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    s.f("confirm.size", model.sizeText, s["tier." + model.tier.id]),
                    fontSize = 13.sp,
                )
                Text(s.f("confirm.ram", model.tier.ramMb), fontSize = 13.sp)
                Text(model.notes, fontSize = 13.sp)
                Text(s.f("confirm.license", model.license), fontSize = 12.sp, color = Muted)
                Spacer(Modifier.height(8.dp))
                if (enough) {
                    Text(
                        s.f(
                            "confirm.spaceOk",
                            formatSizeMb(freeMb),
                            formatSizeMb(requiredMb),
                        ),
                        fontSize = 12.sp,
                    )
                } else {
                    Text(
                        s.f(
                            "confirm.spaceNotEnough",
                            formatSizeMb(freeMb),
                            formatSizeMb(requiredMb),
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = enough,
                onClick = {
                    onDismiss()
                    controller.downloadModel(model)
                },
            ) { Text(s["action.download"]) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s["action.cancel"]) }
        },
    )
}

@Composable
private fun BoxScope.RequirementsPanel(controller: AppController) {
    val s = LocalStrings.current
    val checks = controller.diagnostics
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { controller.closeDiagnostics() }
    )
    Card(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth(0.9f)
            .heightIn(max = 620.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 12.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(s["diag.title"], fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(s["diag.subtitle"], fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
            Divider(Modifier.padding(vertical = 10.dp))
            val listState = rememberLazyListState()
            Box(Modifier.weight(1f, fill = false).heightIn(max = 430.dp)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                ) {
                    items(checks) { check ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                text = when (check.state) {
                                    CheckState.OK -> "✔"
                                    CheckState.WARNING -> "⚠"
                                    CheckState.ERROR -> "✖"
                                },
                                fontSize = 15.sp,
                                color = when (check.state) {
                                    CheckState.OK -> OkGreen
                                    CheckState.WARNING -> WarnAmber
                                    CheckState.ERROR -> MaterialTheme.colors.error
                                },
                                modifier = Modifier.width(24.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(check.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(check.detail, fontSize = 12.sp)
                                check.hint?.let { hint ->
                                    Spacer(Modifier.height(4.dp))
                                    SelectionContainer {
                                        Text(
                                            hint,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF33475B),
                                            lineHeight = 17.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { controller.refreshDiagnostics() }) {
                    Text(s["action.checkAgain"])
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { controller.closeDiagnostics() }) { Text(s["action.close"]) }
            }
        }
    }
}

// ------------------------------------------------------------- progress and log

@Composable
private fun DownloadBar(controller: AppController) {
    val s = LocalStrings.current
    if (!controller.busy) {
        Spacer(Modifier.height(8.dp))
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val fraction = controller.downloadFraction
        if (fraction == null) {
            CircularProgressIndicator(Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
        } else {
            LinearProgressIndicator(progress = fraction, modifier = Modifier.weight(1f))
        }
        Text(controller.downloadLabel, fontSize = 12.sp, color = Muted)
        TextButton(onClick = { controller.cancelDownload() }) {
            Text(s["action.cancel"], fontSize = 12.sp)
        }
    }
}

@Composable
private fun LogPane(controller: AppController) {
    val s = LocalStrings.current
    if (!controller.config.logVisible) {
        val last = controller.logLines.lastOrNull() ?: return
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(s["log.lastLabel"], fontSize = 11.sp, color = Muted)
            Spacer(Modifier.width(8.dp))
            LtrText(
                last,
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                color = Muted,
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(controller.logLines.size) {
        if (controller.logLines.isNotEmpty()) {
            listState.animateScrollToItem(controller.logLines.size - 1)
        }
    }
    Spacer(Modifier.height(10.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(Color(0xFFF8F9FC), RoundedCornerShape(10.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(10.dp)) {
            items(controller.logLines) { line ->
                LtrText(line, fontSize = 11.sp, color = Color(0xFF333333))
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

// ------------------------------------------------------------------------ helpers

private fun pickZipFile(): File? {
    return try {
        val dialog = java.awt.FileDialog(
            null as java.awt.Frame?,
            "Import a Vosk model (.zip)",
            java.awt.FileDialog.LOAD,
        )
        dialog.setFilenameFilter { _, name -> name.endsWith(".zip", ignoreCase = true) }
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val file = dialog.file ?: return null
        File(directory, file)
    } catch (e: Exception) {
        System.err.println("File picker failed: ${e.message}")
        null
    }
}

private fun pickDirectory(): File? {
    return try {
        val chooser = JFileChooser(AppPaths.modelsDir())
        chooser.dialogTitle = "Choose the models folder"
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.isAcceptAllFileFilterUsed = false
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile
        } else {
            null
        }
    } catch (e: Exception) {
        System.err.println("Folder picker failed: ${e.message}")
        null
    }
}

private fun openFolder(folder: File) {
    try {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(folder)
        }
    } catch (e: Exception) {
        System.err.println("Could not open ${folder.absolutePath}: ${e.message}")
    }
}
