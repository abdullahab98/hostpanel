package com.example.data.api

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketManager {

    private var webSocket: WebSocket? = null

    fun streamLogs(baseUrl: String, apiKey: String, projectName: String): Flow<String> {
        return callbackFlow {
            val wsUrl = baseUrl
                .replace("https://", "wss://")
                .replace("http://", "ws://")
                .trimEnd('/') + "/api/containers/$projectName/logs/ws"

            val client = ApiClient.getOkHttpClient(apiKey)
            val request = Request.Builder().url(wsUrl).build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    trySend(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    trySend("[ERROR] WebSocket failed: ${t.message}")
                    close(t)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    trySend("[INFO] Connection closed: $reason")
                    close()
                }

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    trySend("[INFO] Connected to log stream for $projectName")
                }
            })

            awaitClose {
                webSocket?.close(1000, "Composable disposed")
                webSocket = null
            }
        }
    }

    fun streamDeployLogs(baseUrl: String, apiKey: String, projectName: String): Flow<String> {
        return callbackFlow {
            val wsUrl = baseUrl
                .replace("https://", "wss://")
                .replace("http://", "ws://")
                .trimEnd('/') + "/api/deploy/$projectName/logs/ws"

            val client = ApiClient.getOkHttpClient(apiKey)
            val request = Request.Builder().url(wsUrl).build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    trySend(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    trySend("[ERROR] Build stream failed: ${t.message}")
                    close(t)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (reason.contains("done", ignoreCase = true) ||
                        reason.contains("success", ignoreCase = true)) {
                        trySend("[SUCCESS] Deployment complete!")
                    }
                    close()
                }

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    trySend("[INFO] Build log stream connected...")
                }
            })

            awaitClose {
                webSocket?.close(1000, "Composable disposed")
                webSocket = null
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Manually disconnected")
        webSocket = null
    }
}
