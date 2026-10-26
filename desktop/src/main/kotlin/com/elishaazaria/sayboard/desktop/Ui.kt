package com.elishaazaria.sayboard.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.graphics.vector.ImageVector
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

// ------------------------------------------------------------------------ theme

private val PageBackground = Color(0xFFF4F6F9)
private val Ink = Color(0xFF171A21)
private val Teal = Color(0xFF00796B)
private val Maroon = Color(0xFF8E2A2A)
private val Line = Color(0xFFE5E8EE)
private val MutedInk = Color(0xFF5C6373)
private val OkGreen = Color(0xFF1B7F3B)
private val WarnAmber = Color(0xFFB26A00)

private val AppFontFamily = FontFamily(
    androidx.compose.ui.text.platform.Font("fonts/Vazirmatn-Regular.ttf", FontWeight.Normal),
    androidx.compose.ui.text.platform.Font("fonts/Vazirmatn-Medium.ttf", FontWeight.Medium),
    androidx.compose.ui.text.platform.Font("fonts/Vazirmatn-Bold.ttf", FontWeight.Bold),
)

private val AppTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = AppFontFamily),
        displayMedium = displayMedium.copy(fontFamily = AppFontFamily),
        displaySmall = displaySmall.copy(fontFamily = AppFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = AppFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = AppFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = AppFontFamily),
        titleLarge = titleLarge.copy(fontFamily = AppFontFamily),
        titleMedium = titleMedium.copy(fontFamily = AppFontFamily),
        titleSmall = titleSmall.copy(fontFamily = AppFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = AppFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = AppFontFamily),
        bodySmall = bodySmall.copy(fontFamily = AppFontFamily),
        labelLarge = labelLarge.copy(fontFamily = AppFontFamily),
        labelMedium = labelMedium.copy(fontFamily = AppFontFamily),
        labelSmall = labelSmall.copy(fontFamily = AppFontFamily),
    )
}

private val AppColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EDE8),
    onPrimaryContainer = Color(0xFF00382F),
    secondary = Maroon,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E1E1),
    onSecondaryContainer = Color(0xFF400B0B),
    tertiary = Color(0xFF3F51B5),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1E4FA),
    onTertiaryContainer = Color(0xFF16204F),
    background = PageBackground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFB5BCCA),
    outlineVariant = Line,
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFCEDEC),
    onErrorContainer = Color(0xFF5F1510),
)

private val CardShape = RoundedCornerShape(22.dp)

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

private enum class Screen { MODELS, TRANSCRIBE, SETTINGS, ABOUT }

// ------------------------------------------------------------------------ shell

@Composable
fun AppScreen(controller: AppController) {
    val s = controller.strings
    val direction = if (s.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalStrings provides s, LocalLayoutDirection provides direction) {
        MaterialTheme(colorScheme = AppColors, typography = AppTypography) {
            var screen by remember { mutableStateOf(Screen.MODELS) }
            var pendingDownload by remember { mutableStateOf<CatalogModel?>(null) }
            var pendingFolder by remember { mutableStateOf<File?>(null) }

            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxSize()) {
                        SideRail(screen) { screen = it }
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            TopBar(controller, screen)
                            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
                                when (screen) {
                                    Screen.MODELS -> ModelsScreen(controller) { pendingDownload = it }
                                    Screen.TRANSCRIBE -> TranscribeScreen(controller)
                                    Screen.SETTINGS -> SettingsScreen(controller) { pendingFolder = it }
                                    Screen.ABOUT -> AboutScreen(controller)
                                }
                            }
                            Footer(controller)
                        }
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

@Composable
private fun SideRail(screen: Screen, onSelect: (Screen) -> Unit) {
    val s = LocalStrings.current
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight().width(108.dp),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
            ) {
                Image(
                    painter = painterResource("icons/saytypist-mark.png"),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(AppInfo.APP_NAME, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
    ) {
        RailItem(s["nav.models"], Icons.Default.Folder, screen == Screen.MODELS) {
            onSelect(Screen.MODELS)
        }
        RailItem(s["nav.transcribe"], Icons.Default.LibraryMusic, screen == Screen.TRANSCRIBE) {
            onSelect(Screen.TRANSCRIBE)
        }
        Spacer(Modifier.weight(1f))
        RailItem(s["nav.settings"], Icons.Default.Settings, screen == Screen.SETTINGS) {
            onSelect(Screen.SETTINGS)
        }
        RailItem(s["nav.about"], Icons.Default.Info, screen == Screen.ABOUT) {
            onSelect(Screen.ABOUT)
        }
        Text(
            Version.NAME,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 14.dp),
        )
    }
}

@Composable
private fun RailItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label, fontSize = 11.sp, maxLines = 1) },
        alwaysShowLabel = true,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun TopBar(controller: AppController, screen: Screen) {
    val s = LocalStrings.current
    val (title, subtitle) = when (screen) {
        Screen.MODELS -> s["nav.models"] to s["models.hint"]
        Screen.TRANSCRIBE -> s["transcribe.title"] to s["transcribe.subtitle"]
        Screen.SETTINGS -> s["nav.settings"] to s["settings.immediate"]
        Screen.ABOUT -> s["about.title"] to s.f("about.version", Version.NAME)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        StatusPill(controller.status)
        Spacer(Modifier.width(10.dp))
        LanguageButton(controller)
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = { controller.toggleLog() }) {
            Icon(
                if (controller.config.logVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (controller.config.logVisible) s["header.hideLog"] else s["header.showLog"],
            )
        }
        Spacer(Modifier.width(6.dp))
        val listening = controller.status == RecognitionState.LISTENING ||
                controller.status == RecognitionState.LOADING
        Button(
            onClick = { controller.toggleDictation() },
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(44.dp),
        ) {
            Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (listening) s["action.stop"] else s["action.start"], fontSize = 13.sp)
        }
    }
}

@Composable
private fun StatusPill(state: RecognitionState) {
    val s = LocalStrings.current
    val (label, color) = when (state) {
        RecognitionState.IDLE -> s["status.idle"] to MutedInk
        RecognitionState.LOADING -> s["status.loading"] to WarnAmber
        RecognitionState.LISTENING -> s["status.listening"] to OkGreen
        RecognitionState.ERROR -> s["status.error"] to MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.30f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Box(Modifier.size(7.dp).background(color, CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LanguageButton(controller: AppController) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, shape = CircleShape, modifier = Modifier.height(36.dp)) {
            Text(s.lang.label, fontSize = 12.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Lang.values().forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.label, fontSize = 13.sp) },
                    onClick = {
                        expanded = false
                        controller.setLanguage(lang)
                    },
                )
            }
        }
    }
}

// ----------------------------------------------------------------------- footer

@Composable
private fun Footer(controller: AppController) {
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        if (controller.busy) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                val fraction = controller.downloadFraction
                if (fraction == null) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    LinearProgressIndicator(progress = fraction, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.width(12.dp))
                Text(controller.downloadLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { controller.cancelDownload() }) { Text(s["action.cancel"], fontSize = 12.sp) }
            }
        }
        LogPane(controller)
    }
}

@Composable
private fun LogPane(controller: AppController) {
    val s = LocalStrings.current
    if (!controller.config.logVisible) {
        val last = controller.logLines.lastOrNull() ?: return
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            Text(s["log.lastLabel"], fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            LtrText(
                last,
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(controller.logLines.size) {
        if (controller.logLines.isNotEmpty()) listState.animateScrollToItem(controller.logLines.size - 1)
    }
    Surface(
        color = Color(0xFF101318),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(150.dp).padding(bottom = 14.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(12.dp)) {
                items(controller.logLines) { line ->
                    LtrText(line, fontSize = 11.sp, color = Color(0xFFC9D1D9), fontFamily = FontFamily.Monospace)
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }
}

// ----------------------------------------------------------------------- models

@Composable
private fun ModelsScreen(controller: AppController, onLargeDownload: (CatalogModel) -> Unit) {
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

    Column(Modifier.fillMaxSize()) {
        PreviewLine(controller)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Card(
            modifier = Modifier.weight(0.40f).fillMaxHeight(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Line),
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s["models.installed"], fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    TinyChip("${controller.installed.size} · ${controller.totalModelsSize()}")
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { controller.refreshInstalled() }) {
                        Icon(Icons.Default.Tune, s["action.refresh"], Modifier.size(18.dp))
                    }
                    IconButton(onClick = { pickZipFile()?.let { controller.importZip(it) } }) {
                        Icon(Icons.Default.FolderOpen, s["action.importZip"], Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (controller.installed.isEmpty()) {
                    EmptyHint(Icons.Default.Folder, s["models.none"])
                } else {
                    val listState = rememberLazyListState()
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
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
            modifier = Modifier.weight(0.60f).fillMaxHeight(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Line),
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s["models.catalog"], fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    TinyChip("${filtered.size}")
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        singleLine = true,
                        placeholder = { Text(s["models.search"], fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(FIELD_HEIGHT),
                    )
                    Spacer(Modifier.width(10.dp))
                    UiDropdown(
                        selected = if (language.isBlank()) s["models.allLanguages"] else language,
                        options = listOf(s["models.allLanguages"] to "") +
                                ModelCatalog.languages.map { it to it },
                        modifier = Modifier.width(200.dp).height(FIELD_HEIGHT),
                    ) { language = it }
                }
                Spacer(Modifier.height(10.dp))
                val listState = rememberLazyListState()
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.id }) { model ->
                            AvailableModelRow(model, model.id in installedNames) {
                                if (model.isHeavy) onLargeDownload(model) else controller.downloadModel(model)
                            }
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
}

private val FIELD_HEIGHT = 56.dp

@Composable
private fun TinyChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
    ) {
        Text(
            text,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun EmptyHint(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

@Composable
private fun UiDropdown(
    selected: String,
    options: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(selected, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, null, Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 13.sp) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun InstalledModelCard(controller: AppController, model: InstalledModel) {
    val s = LocalStrings.current
    var confirmDelete by remember { mutableStateOf(false) }
    val active = controller.config.model == model.name
    Surface(
        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    LtrText(model.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LtrText(
                            "${model.lang}  ·  ${model.sizeText}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (active) {
                            Spacer(Modifier.width(8.dp))
                            TinyChip(s["models.activeTag"])
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = { controller.setActiveModel(model.name) }, enabled = !active) {
                    Text(s["action.use"], fontSize = 12.sp)
                }
                TextButton(onClick = { controller.exportZip(model) }) {
                    Text(s["action.exportZip"], fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, s["action.delete"], Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(s["models.deleteTitle"], fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text(s.f("models.deleteText", model.name, model.sizeText), fontSize = 13.sp) },
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                LtrText(model.id, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                LtrText(
                    "${model.sizeText}  ·  ${s["tier." + model.tier.id]}  ·  ${model.license}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LtrText(model.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f))
            }
            Spacer(Modifier.width(10.dp))
            if (installed) {
                TinyChip(s["models.installedTag"])
            } else {
                FilledTonalButton(
                    onClick = onDownload,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(34.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(Icons.Default.Download, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s["action.download"], fontSize = 12.sp)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ transcribe

@Composable
private fun TranscribeScreen(controller: AppController) {
    val s = LocalStrings.current
    val file = controller.transcriptionFile
    val transcript = controller.transcript
    val running = controller.transcriptionRunning
    val progress = controller.transcriptionProgress

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Card(
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Line),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            Icons.Default.LibraryMusic,
                            null,
                            Modifier.padding(10.dp).size(22.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (file == null) {
                            Text(s["transcribe.noFile"], fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LtrText(file.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            LtrText(
                                s.f(
                                    "transcribe.fileInfo",
                                    formatBytes(file.length()),
                                    file.extension.uppercase(),
                                    formatTimecode(AudioFileReader.durationSeconds(file)),
                                ),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TinyChip(s.f("transcribe.model", controller.config.model.ifBlank { "—" }))
                            if (controller.transcriptionUsesFfmpeg) {
                                Spacer(Modifier.width(6.dp))
                                TinyChip("ffmpeg")
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { pickAudioFile()?.let { controller.chooseTranscriptionFile(it) } },
                        enabled = !running,
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Default.FolderOpen, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s["transcribe.choose"], fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (running) {
                        FilledTonalButton(onClick = { controller.cancelTranscription() }, shape = RoundedCornerShape(50)) {
                            Icon(Icons.Default.Stop, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s["transcribe.cancel"], fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { controller.startTranscription() },
                            enabled = file != null,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(s["transcribe.start"], fontSize = 12.sp)
                        }
                    }
                }
                controller.transcriptionError?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                if (running) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (progress == null) {
                            LinearProgressIndicator(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                        } else {
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (progress == null) s["transcribe.loadingModel"]
                            else s.f("transcribe.working", (progress * 100).toInt()),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Card(
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Line),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s["transcribe.result"], fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    transcript?.let {
                        TinyChip(
                            s.f(
                                "transcribe.resultInfo",
                                it.lines.size,
                                it.wordCount,
                                formatTimecode(it.durationSeconds),
                            )
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { controller.copyTranscript(withTimecodes = false) },
                        enabled = transcript != null,
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s["transcribe.copy"], fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { controller.copyTranscript(withTimecodes = true) },
                        enabled = transcript != null,
                    ) { Text(s["transcribe.copyTimed"], fontSize = 12.sp) }
                    TextButton(
                        onClick = {
                            saveTextFile(controller.suggestedTranscriptName())?.let { controller.saveTranscript(it) }
                        },
                        enabled = transcript != null,
                    ) { Text(s["transcribe.save"], fontSize = 12.sp) }
                }
                Spacer(Modifier.height(8.dp))
                if (transcript == null) {
                    EmptyHint(Icons.Default.LibraryMusic, s["transcribe.empty"])
                } else {
                    val listState = rememberLazyListState()
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(transcript.lines) { line ->
                                Row(Modifier.fillMaxWidth()) {
                                    LtrText(
                                        formatTimecode(line.startSeconds),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(64.dp),
                                    )
                                    SelectionContainer {
                                        Text(line.text, fontSize = 13.sp, color = Ink)
                                    }
                                }
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
}

// ---------------------------------------------------------------------- settings

@Composable
private fun SettingsScreen(controller: AppController, onPickFolder: (File) -> Unit) {
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
    var copyToClipboard by remember(config.copyToClipboard) { mutableStateOf(config.copyToClipboard) }
    var startMinimized by remember(config.startMinimized) { mutableStateOf(config.startMinimized) }
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(s["settings.typing"]) {
            FieldRow(s["settings.hotkey"]) {
                OutlinedTextField(
                    value = hotkey,
                    onValueChange = { hotkey = it },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.width(220.dp).height(FIELD_HEIGHT),
                )
                Spacer(Modifier.width(10.dp))
                Button(onClick = { save() }, shape = RoundedCornerShape(50)) {
                    Text(s["action.saveHotkey"], fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                LtrText(s["settings.hotkeyHint"], fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FieldRow(s["settings.microphone"]) {
                UiDropdown(device, devices.map { it to it }, Modifier.width(330.dp).height(FIELD_HEIGHT)) {
                    device = it
                    save(validateHotkey = false)
                }
            }
            FieldRow(s["settings.typingMethod"]) {
                UiDropdown(backend, Typers.available().map { it to it }, Modifier.width(200.dp).height(FIELD_HEIGHT)) {
                    backend = it
                    save(validateHotkey = false)
                }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = { controller.testTyping() }, shape = RoundedCornerShape(50)) {
                    Text(s["action.testTyping"], fontSize = 12.sp)
                }
            }
        }

        SectionCard(s["settings.behaviour"]) {
            ToggleRow(s["settings.typeLive"], livePartial) { livePartial = it; save(validateHotkey = false) }
            ToggleRow(s["settings.trailingSpace"], trailingSpace) { trailingSpace = it; save(validateHotkey = false) }
            ToggleRow(s["settings.capitalize"], capitalize) { capitalize = it; save(validateHotkey = false) }
            ToggleRow(s["settings.keepModel"], keepModel) { keepModel = it; save(validateHotkey = false) }
            ToggleRow(s["settings.showPreview"], showPreview) { showPreview = it; save(validateHotkey = false) }
            ToggleRow(s["settings.copyPhrases"], copyToClipboard) { copyToClipboard = it; save(validateHotkey = false) }
            ToggleRow(s["settings.startMinimized"], startMinimized) { startMinimized = it; save(validateHotkey = false) }
        }

        SectionCard(s["settings.storage"]) {
            Text(s["settings.storageText"], fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    SelectionContainer {
                        LtrText(
                            controller.modelsFolder().absolutePath,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Text(
                        s.f("settings.freeSpace", formatSizeMb(AppPaths.freeSpaceMb())),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { pickDirectory()?.let(onPickFolder) }, shape = RoundedCornerShape(50)) {
                    Text(s["action.changeFolder"], fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { openFolder(controller.modelsFolder()) }, shape = RoundedCornerShape(50)) {
                    Icon(Icons.Default.FolderOpen, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s["action.openFolder"], fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { controller.unloadModel() }) {
                    Text(s["settings.unload"], fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            LtrText(
                s.f("settings.settingsFile", AppPaths.configFile().absolutePath),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(s["settings.requirements"]) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { controller.refreshDiagnostics() }) { Text(s["action.checkAgain"], fontSize = 12.sp) }
                TextButton(onClick = { controller.openDiagnostics() }) { Text(s["action.showDetails"], fontSize = 12.sp) }
            }
            controller.diagnostics.forEach { check -> CheckRowLine(check) }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun CheckRowLine(check: DiagnosticCheck) {
    Row(Modifier.padding(vertical = 2.dp)) {
        val (icon, color) = when (check.state) {
            CheckState.OK -> Icons.Default.Folder to OkGreen
            CheckState.WARNING -> Icons.Default.Warning to WarnAmber
            CheckState.ERROR -> Icons.Default.ErrorOutline to MaterialTheme.colorScheme.error
        }
        Icon(
            if (check.state == CheckState.OK) Icons.Default.Info else icon,
            null,
            Modifier.size(15.dp),
            tint = color,
        )
        Spacer(Modifier.width(8.dp))
        Text("${check.title}: ${check.detail}", fontSize = 12.sp)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FieldRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, modifier = Modifier.width(150.dp))
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Switch(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp)
    }
}

// ------------------------------------------------------------------------- about

@Composable
private fun AboutScreen(controller: AppController) {
    val s = LocalStrings.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(s["about.title"]) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource("icons/saytypist.png"),
                    contentDescription = null,
                    modifier = Modifier.width(180.dp).height(110.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(s.f("about.version", Version.NAME), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(s["about.description"], fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(s["about.howItWorks"], fontSize = 13.sp, lineHeight = 20.sp)
        }
        SectionCard(s["about.developer"]) {
            Text(AppInfo.MAINTAINER, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (AppInfo.MAINTAINER_URL.isNotBlank()) {
                LtrText(AppInfo.MAINTAINER_URL, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            Text(s["about.upstream"], fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(s.f("about.license", AppInfo.LICENSE), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard(s["about.credits"]) {
            Text(s["about.creditsText"], fontSize = 12.sp, lineHeight = 18.sp)
        }
        SectionCard(s["about.data"]) {
            SelectionContainer {
                LtrText(
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
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", fontSize = 12.sp)
        SelectionContainer { LtrText(url, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) }
    }
}

// -------------------------------------------------------------------- dialogues

@Composable
private fun MoveModelsDialog(controller: AppController, folder: File, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val models = controller.installed
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s["confirm.moveTitle"], fontSize = 17.sp, fontWeight = FontWeight.Bold) },
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
private fun DownloadConfirmDialog(model: CatalogModel, controller: AppController, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val freeMb = remember(model) { AppPaths.freeSpaceMb() }
    val requiredMb = controller.requiredSpaceMb(model)
    val enough = freeMb < 0 || freeMb >= requiredMb
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (model.isHeavy) s["confirm.titleHeavy"] else s["confirm.title"],
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                LtrText(model.id, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(s.f("confirm.size", model.sizeText, s["tier." + model.tier.id]), fontSize = 13.sp)
                Text(s.f("confirm.ram", model.tier.ramMb), fontSize = 13.sp)
                Text(model.notes, fontSize = 13.sp)
                Text(s.f("confirm.license", model.license), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                if (enough) {
                    Text(s.f("confirm.spaceOk", formatSizeMb(freeMb), formatSizeMb(requiredMb)), fontSize = 12.sp)
                } else {
                    Text(
                        s.f("confirm.spaceNotEnough", formatSizeMb(freeMb), formatSizeMb(requiredMb)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(s["action.cancel"]) } },
    )
}

@Composable
private fun BoxScope.RequirementsPanel(controller: AppController) {
    val s = LocalStrings.current
    val checks = controller.diagnostics
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable { controller.closeDiagnostics() }
    )
    Card(
        modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.86f).heightIn(max = 640.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(s["diag.title"], fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(
                s["diag.subtitle"],
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(12.dp))
            val listState = rememberLazyListState()
            Box(Modifier.weight(1f, fill = false).heightIn(max = 430.dp)) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
                    items(checks) { check ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                            val (icon, color) = when (check.state) {
                                CheckState.OK -> Icons.Default.Info to OkGreen
                                CheckState.WARNING -> Icons.Default.Warning to WarnAmber
                                CheckState.ERROR -> Icons.Default.ErrorOutline to MaterialTheme.colorScheme.error
                            }
                            Icon(icon, null, Modifier.size(18.dp), tint = color)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(check.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(check.detail, fontSize = 12.sp)
                                check.hint?.let { hint ->
                                    Spacer(Modifier.height(5.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        SelectionContainer {
                                            LtrText(
                                                hint,
                                                modifier = Modifier.padding(10.dp),
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
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { controller.refreshDiagnostics() }) { Text(s["action.checkAgain"]) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { controller.closeDiagnostics() }) { Text(s["action.close"]) }
            }
        }
    }
}

// ------------------------------------------------------------------------ preview
// (the live preview lives in the top bar of the Models screen)

@Composable
private fun PreviewLine(controller: AppController) {
    val s = LocalStrings.current
    if (!controller.config.showPreview) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Text(
            controller.preview.ifBlank { s["preview.hint"] },
            color = if (controller.preview.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else OkGreen,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { controller.copyLastPhrase() },
            enabled = controller.lastPhrase.isNotBlank() || controller.preview.isNotBlank(),
        ) { Text(s["action.copyLast"], fontSize = 12.sp) }
    }
}

// ------------------------------------------------------------------------ helpers

private fun pickZipFile(): File? = pickFile(
    title = "Import a Vosk model (.zip)",
    filter = { name -> name.endsWith(".zip", ignoreCase = true) },
)

private fun pickAudioFile(): File? = pickFile(
    title = "Choose an audio file",
    // show everything: ffmpeg reads the container itself, and some recorders use
    // extensions that are not in the list (e.g. .ogx)
    filter = { true },
)

private fun pickFile(title: String, filter: (String) -> Boolean): File? {
    return try {
        val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
        dialog.setFilenameFilter { _, name -> filter(name) }
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val file = dialog.file ?: return null
        File(directory, file)
    } catch (e: Exception) {
        System.err.println("File picker failed: ${e.message}")
        null
    }
}

private fun saveTextFile(suggestedName: String): File? {
    return try {
        val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Save transcript", java.awt.FileDialog.SAVE)
        dialog.file = suggestedName
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val file = dialog.file ?: return null
        File(directory, file)
    } catch (e: Exception) {
        System.err.println("Save dialog failed: ${e.message}")
        null
    }
}

private fun pickDirectory(): File? {
    return try {
        val chooser = JFileChooser(AppPaths.modelsDir())
        chooser.dialogTitle = "Choose the models folder"
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.isAcceptAllFileFilterUsed = false
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    } catch (e: Exception) {
        System.err.println("Folder picker failed: ${e.message}")
        null
    }
}

private fun openFolder(folder: File) {
    try {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(folder)
    } catch (e: Exception) {
        System.err.println("Could not open ${folder.absolutePath}: ${e.message}")
    }
}
