package com.example.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

object TermuxBridge {

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun startControlPlane(context: Context) {
        if (!isTermuxInstalled(context)) {
            Toast.makeText(context, "Termux is not installed", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", "cd ~/hostpanel-control-plane && npm start"))
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            context.startService(intent)
            Toast.makeText(context, "Starting HostPanel Control Plane in Termux...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start Termux service. Make sure Termux:API is installed or RUN_COMMAND is allowed in termux.properties", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun openTermux(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage("com.termux")
        if (intent != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Termux is not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
