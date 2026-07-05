package com.example.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

class SimpleWebServer(port: Int, private val wwwroot: File) : NanoHTTPD(port) {

    init {
        Log.d("SimpleWebServer", "Server initialized on port $port, root: ${wwwroot.absolutePath}")
    }

    override fun serve(session: IHTTPSession): Response {
        var uri = session.uri
        Log.d("SimpleWebServer", "Requested URI: $uri")

        // Basic security to prevent directory traversal
        if (uri.contains("../")) {
            return newFixedLengthResponse(
                Response.Status.FORBIDDEN, MIME_PLAINTEXT,
                "Forbidden: Directory traversal not allowed."
            )
        }

        // Default to index.html
        if (uri.endsWith("/")) {
            uri += "index.html"
        }

        val requestedFile = File(wwwroot, uri)

        if (!requestedFile.exists() || requestedFile.isDirectory) {
            // Try to serve index.html if it's a directory without trailing slash
            val indexFile = File(requestedFile, "index.html")
            if (indexFile.exists() && indexFile.isFile) {
                return serveFile(indexFile)
            }
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                "Error 404: File not found."
            )
        }

        return serveFile(requestedFile)
    }

    private fun serveFile(file: File): Response {
        try {
            val mimeType = getMimeTypeCustom(file.name)
            val fileInputStream = FileInputStream(file)
            return newChunkedResponse(Response.Status.OK, mimeType, fileInputStream)
        } catch (e: FileNotFoundException) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                "Error 404: File not found."
            )
        }
    }

    private fun getMimeTypeCustom(fileName: String): String {
        return when {
            fileName.endsWith(".html") || fileName.endsWith(".htm") -> "text/html"
            fileName.endsWith(".css") -> "text/css"
            fileName.endsWith(".js") -> "application/javascript"
            fileName.endsWith(".json") -> "application/json"
            fileName.endsWith(".png") -> "image/png"
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
            fileName.endsWith(".gif") -> "image/gif"
            fileName.endsWith(".svg") -> "image/svg+xml"
            fileName.endsWith(".txt") -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
