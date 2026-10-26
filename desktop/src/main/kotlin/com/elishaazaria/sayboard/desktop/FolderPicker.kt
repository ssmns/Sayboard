package com.elishaazaria.sayboard.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File

/**
 * A folder chooser that matches the rest of the window (Material 3), instead of
 * the grey Swing file chooser. It navigates with a breadcrumb, can create
 * folders and shows how many items a folder has.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerDialog(
    title: String,
    start: File,
    onDismiss: () -> Unit,
    onSelect: (File) -> Unit,
) {
    val s = LocalStrings.current
    var current by remember { mutableStateOf(start.takeIf { it.isDirectory } ?: File(System.getProperty("user.home"))) }
    var showHidden by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val children = remember(current, showHidden) {
        current.listFiles()
            ?.filter { it.isDirectory && (showHidden || !it.name.startsWith(".")) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    Dialog(
        onCloseRequest = onDismiss,
        title = title,
        state = rememberDialogState(size = DpSize(760.dp, 580.dp)),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FolderOpen,
                        null,
                        Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))

                // place bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp)) {
                        IconButton(onClick = { current.parentFile?.let { current = it } }, enabled = current.parentFile != null) {
                            Icon(Icons.Default.ArrowUpward, s["folder.up"], Modifier.size(18.dp))
                        }
                        IconButton(onClick = { current = File(System.getProperty("user.home")) }) {
                            Icon(Icons.Default.Home, s["folder.home"], Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        LtrText(
                            current.absolutePath,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilterChip(
                            selected = showHidden,
                            onClick = { showHidden = !showHidden },
                            label = { Text(s["folder.hidden"], fontSize = 11.sp) },
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { newFolderName = "" }) {
                            Icon(Icons.Default.CreateNewFolder, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s["folder.new"], fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (children.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(s["folder.empty"], fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val listState = rememberLazyListState()
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(children, key = { it.absolutePath }) { folder ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { current = folder; error = null }
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            RoundedCornerShape(10.dp),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        null,
                                        Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(folder.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    if (folder.canWrite().not()) {
                                        Text(s["folder.readOnly"], fontSize = 10.sp, color = Color(0xFFB3261E))
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

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(s["action.cancel"]) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSelect(current) }, shape = RoundedCornerShape(50)) {
                        Text(s["folder.use"])
                    }
                }
            }
        }
    }

    newFolderName?.let { name ->
        Dialog(onCloseRequest = { newFolderName = null }, title = s["folder.new"]) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(20.dp).width(420.dp)) {
                    Text(s["folder.new"], fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newFolderName = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            createNewFolder(current, name)?.let { created ->
                                current = created
                                error = null
                            } ?: run { error = s["folder.createFailed"] }
                            newFolderName = null
                        }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { newFolderName = null }) { Text(s["action.cancel"]) }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            createNewFolder(current, name)?.let { created ->
                                current = created
                                error = null
                            } ?: run { error = s["folder.createFailed"] }
                            newFolderName = null
                        }) { Text(s["folder.create"]) }
                    }
                }
            }
        }
    }
}

/** Creates the folder (if needed) and returns it, or null on failure. */
private fun createNewFolder(parent: File, name: String): File? {
    val cleaned = name.trim()
    if (cleaned.isEmpty()) return null
    val target = File(parent, cleaned)
    if (target.isDirectory) return target
    return if (!target.exists() && target.mkdirs()) target else null
}
