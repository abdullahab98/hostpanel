package com.example.ui.screen

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    val settings = remember { com.example.data.datastore.SettingsDataStore(context) }
    val apiKey by settings.apiKey.collectAsState(initial = "hostpanel-local")
    val themeMode by settings.themeMode.collectAsState(initial = "dark")
    
    // We will inject a full HTML page with xterm.js loaded from CDN
    // It will connect to our local control plane's WebSocket
    val htmlContent = remember(apiKey, themeMode) {
        val bg = if (themeMode == "light") "#FFFFFF" else "#000000"
        val fg = if (themeMode == "light") "#121824" else "#FFFFFF"
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>Terminal</title>
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/xterm@5.3.0/css/xterm.css" />
            <script src="https://cdn.jsdelivr.net/npm/xterm@5.3.0/lib/xterm.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/xterm-addon-fit@0.8.0/lib/xterm-addon-fit.js"></script>
            <style>
                body { margin: 0; padding: 0; background-color: $bg; overflow: hidden; height: 100vh; width: 100vw; }
                #terminal-container { height: 100%; width: 100%; padding: 4px; }
                .xterm .xterm-viewport { overflow-y: auto !important; }
            </style>
        </head>
        <body>
            <div id="terminal-container"></div>
            <script>
                var term = new Terminal({
                    cursorBlink: true,
                    theme: { background: '$bg', foreground: '$fg' },
                    fontSize: 14,
                    fontFamily: 'monospace'
                });
                var fitAddon = new FitAddon.FitAddon();
                term.loadAddon(fitAddon);
                term.open(document.getElementById('terminal-container'));
                fitAddon.fit();
                
                var apiKey = '$apiKey';
                
                var ws = new WebSocket('ws://localhost:3001/ws/terminal?token=' + apiKey + '&cols=' + term.cols + '&rows=' + term.rows);
                
                ws.onopen = function() {
                    // connected
                };
                
                ws.onmessage = function(event) {
                    term.write(event.data);
                };
                
                ws.onerror = function() {
                    term.write('\r\n\x1b[31m[WebSocket Error] Cannot connect to Termux Control Plane on localhost:3001\x1b[0m\r\n');
                };
                
                ws.onclose = function() {
                    term.write('\r\n\x1b[33m[Connection closed]\x1b[0m\r\n');
                };
                
                term.onData(function(data) {
                    if (ws.readyState === WebSocket.OPEN) {
                        ws.send(JSON.stringify({ type: 'input', data: data }));
                    }
                });
                
                window.addEventListener('resize', function() {
                    fitAddon.fit();
                    if (ws.readyState === WebSocket.OPEN) {
                        ws.send(JSON.stringify({ type: 'resize', cols: term.cols, rows: term.rows }));
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Termux Console", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    this.settings.javaScriptEnabled = true
                    this.settings.domStorageEnabled = true
                    this.settings.useWideViewPort = true
                    this.settings.loadWithOverviewMode = true
                    
                    webViewClient = WebViewClient()
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            return true
                        }
                    }
                    
                    // Load the local HTML containing xterm.js
                    loadDataWithBaseURL("http://localhost", htmlContent, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
