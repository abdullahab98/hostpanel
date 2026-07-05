package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.server.SimpleWebServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class LocalViewModel(application: Application) : AndroidViewModel(application) {

    private val port = 8080
    private var webServer: SimpleWebServer? = null
    
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverIpAddress = MutableStateFlow<String?>("127.0.0.1")
    val serverIpAddress: StateFlow<String?> = _serverIpAddress.asStateFlow()

    private val _deployedProjectName = MutableStateFlow<String?>("No Project Uploaded")
    val deployedProjectName: StateFlow<String?> = _deployedProjectName.asStateFlow()

    private val _serverLogs = MutableStateFlow<List<String>>(emptyList())
    val serverLogs: StateFlow<List<String>> = _serverLogs.asStateFlow()

    private val wwwroot: File = File(application.filesDir, "wwwroot").apply {
        if (!exists()) {
            mkdirs()
            // Create a default index.html if empty
            File(this, "index.html").writeText(
                "<html><head><title>Android Host</title></head><body style=\"font-family:sans-serif;text-align:center;margin-top:20%;\"><h1>Welcome to Android Local Host!</h1><p>Upload a .zip file containing your static website in the app.</p></body></html>"
            )
        }
    }

    init {
        logMessage("System initialized. Ready to host.")
        _serverIpAddress.value = getLocalIpAddress()
    }

    fun startServer() {
        if (webServer != null && webServer!!.isAlive) return
        
        try {
            webServer = SimpleWebServer(port, wwwroot)
            webServer?.start()
            _isServerRunning.value = true
            _serverIpAddress.value = getLocalIpAddress()
            logMessage("Server started on port $port")
            logMessage("Accessible at http://${_serverIpAddress.value}:$port")
        } catch (e: Exception) {
            logMessage("Failed to start server: ${e.message}")
            e.printStackTrace()
        }
    }

    fun stopServer() {
        webServer?.stop()
        webServer = null
        _isServerRunning.value = false
        logMessage("Server stopped.")
    }

    fun deployZipFile(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            logMessage("Starting deployment of $fileName...")
            try {
                // Clear existing wwwroot
                wwwroot.deleteRecursively()
                wwwroot.mkdirs()

                // Extract Zip
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry
                var foundFiles = 0

                while (entry != null) {
                    val newFile = File(wwwroot, entry.name)
                    // Security check to prevent zip slip
                    if (!newFile.canonicalPath.startsWith(wwwroot.canonicalPath + File.separator)) {
                        throw SecurityException("Zip entry is outside of the target dir: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        // Ensure parent dir exists
                        newFile.parentFile?.mkdirs()
                        val fos = FileOutputStream(newFile)
                        zipInputStream.copyTo(fos)
                        fos.close()
                        foundFiles++
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
                zipInputStream.close()

                _deployedProjectName.value = fileName
                logMessage("Successfully deployed $fileName ($foundFiles files extracted).")
                
                // Restart server to apply changes if running
                if (_isServerRunning.value) {
                    stopServer()
                    startServer()
                } else {
                    startServer()
                }

            } catch (e: Exception) {
                logMessage("Error deploying zip: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun logMessage(msg: String) {
        Log.d("LocalViewModel", msg)
        val currentLogs = _serverLogs.value.toMutableList()
        currentLogs.add(0, "> $msg")
        if (currentLogs.size > 50) currentLogs.removeLast()
        _serverLogs.value = currentLogs
    }

    private fun getLocalIpAddress(): String {
        try {
            val context = getApplication<Application>()
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ipAddress = wifiManager.connectionInfo.ipAddress
                return String.format(
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff,
                    ipAddress shr 24 and 0xff
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1" // Fallback
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
    }
}
