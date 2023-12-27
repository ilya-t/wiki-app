package com.tsourcecode.wiki.app.storage

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.tsourcecode.wiki.lib.domain.storage.ExternalStorageAccessHandler

/**
 * @see https://developer.android.com/training/data-storage/manage-all-files
 */
class ExternalStorageAccessHandlerImpl(
    private val onAccessGranted: () -> Unit,
) : ExternalStorageAccessHandler {
    private var request: ActivityResultLauncher<Intent>? = null
    private val manageAllFilesRequest = ActivityResultContracts.StartActivityForResult()
    override val accessGranted: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            TODO("VERSION.SDK_INT < R")
        }

    override fun requestAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent().apply {
                action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            }
            request?.launch(intent)
        } else {
            TODO("VERSION.SDK_INT < R")
        }
    }

    init {
        if (accessGranted) {
            onAccessGranted()
        }
    }

    fun bind(activity: ComponentActivity?) {
        if (activity == null) {
            request = null
        } else {
            request = activity.registerForActivityResult(manageAllFilesRequest) {
                if (accessGranted) {
                    onAccessGranted()
                }
            }
            if (!accessGranted) {
                requestAccess()
            }
        }
    }
}
