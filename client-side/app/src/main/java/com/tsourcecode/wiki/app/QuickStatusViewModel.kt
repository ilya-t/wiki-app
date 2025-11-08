package com.tsourcecode.wiki.app

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.tsourcecode.wiki.app.util.Sharing
import com.tsourcecode.wiki.lib.domain.QuickStatus
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.StatusInfo
import com.tsourcecode.wiki.lib.domain.util.DebugLogger

class QuickStatusViewModel(
        private val activity: AppCompatActivity,
        quickStatusController: QuickStatusController,
) {
    private val tvStatus = activity.findViewById<AppCompatTextView>(R.id.tv_status)
    private var lastStatus: StatusInfo? = null

    init {
        quickStatusController.listener = { status ->
            tvStatus.post {
                updateStatus(status)
            }
        }

        tvStatus.setOnClickListener {
            if (tvStatus.layoutParams == null) {
                tvStatus.layoutParams = ViewGroup.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            }
            if (tvStatus.layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                tvStatus.layoutParams.height = activity.resources.getDimensionPixelSize(R.dimen.status_height)
                tvStatus.requestLayout()
            } else {
                tvStatus.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                tvStatus.requestLayout()
                val clipboardManager =
                    activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val error = lastStatus?.error

                if (error != null) {
                    clipboardManager.setPrimaryClip(
                        ClipData(
                            ClipDescription("stack", arrayOf("")),
                            ClipData.Item(lastStatus?.error?.stackTraceToString())
                        )
                    )
                    Toast.makeText(activity, "Stack at clipboard!", Toast.LENGTH_SHORT).show()
                } else {
                    val logsBody: String = DebugLogger.inMemoryLogs.joinToString("\n")
                    clipboardManager.setPrimaryClip(
                        ClipData(
                            ClipDescription("logs", arrayOf("")),
                            ClipData.Item(logsBody)
                        )
                    )
                    Toast.makeText(activity, "Logs at clipboard!", Toast.LENGTH_SHORT).show()
                    Sharing.shareTextAsFile(activity, logsBody).onSuccess {
                        activity.startActivity(it)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateStatus(status: StatusInfo) {
        if (status.error != null) {
            Log.e("QuickStatusViewModel", "unexpected error! (status comment: ${status.comment})", status.error)
            tvStatus.setBackgroundColor(
                    activity.resources.getColor(R.color.status_error)
            )
            tvStatus.text = "${status.status.name}: ${status.error?.message?:"null"}"
        } else {
            tvStatus.setBackgroundColor(
                    activity.resources.getColor(status.status.color())
                    )
            tvStatus.text = if (status.comment.isBlank()) {
                status.status.name
            } else {
                "${status.status.name}: ${status.comment}"
            }
        }

        this.lastStatus = status
    }

    private fun QuickStatus.color(): Int {
        return when (this) {
            QuickStatus.SYNC -> R.color.status_progress
            QuickStatus.DECOMPRESS -> R.color.status_progress
            QuickStatus.SYNCED -> R.color.status_ok
            QuickStatus.STAGE -> R.color.status_progress
            QuickStatus.STAGED -> R.color.status_ok
            QuickStatus.COMMIT -> R.color.status_progress
            QuickStatus.COMMITED -> R.color.status_ok
            QuickStatus.STATUS_UPDATE -> R.color.status_progress
            QuickStatus.STATUS_UPDATED -> R.color.status_ok
            QuickStatus.ERROR -> R.color.status_error
        }
    }
}
