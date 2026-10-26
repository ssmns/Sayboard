package com.elishaazaria.sayboard.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState

fun main() {
    val controller = AppController()
    application {
        val traySupported = isTraySupported
        var windowVisible by remember {
            mutableStateOf(!(controller.config.startMinimized && traySupported))
        }
        var quitting by remember { mutableStateOf(false) }

        if (quitting) {
            LaunchedEffect(Unit) {
                controller.shutdown()
                exitApplication()
            }
        }

        if (traySupported) {
            Tray(
                icon = painterResource("icons/tray.png"),
                tooltip = "sayTypist",
                menu = {
                    Item("Start / stop dictation", onClick = { controller.toggleDictation() })
                    Item("Show window", onClick = { windowVisible = true })
                    Separator()
                    Item("Quit", onClick = { quitting = true })
                }
            )
        }

        if (windowVisible) {
            Window(
                onCloseRequest = {
                    if (traySupported) {
                        windowVisible = false
                    } else {
                        quitting = true
                    }
                },
                title = "sayTypist ${Version.NAME} - voice typing",
                icon = painterResource("icons/saytypist-mark.png"),
                state = rememberWindowState(size = DpSize(1120.dp, 900.dp))
            ) {
                AppScreen(controller)
            }
        }
    }
}
