package com.tsourcecode.wiki.app

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.tsourcecode.wiki.lib.domain.QuickStatus
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.StatusInfo

class QuickStatusViewModel(
        private val activity: AppCompatActivity,
        quickStatusController: QuickStatusController,
) {
    private val tvStatus = activity.findViewById<AppCompatTextView>(R.id.tv_status)

    init {
        quickStatusController.listener = { status ->
            tvStatus.post {
                updateStatus(status)
            }
        }

        tvStatus.setOnClickListener {
            if (tvStatus.layoutParams.height == FrameLayout.LayoutParams.WRAP_CONTENT) {
                tvStatus.layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        activity.resources.getDimensionPixelSize(R.dimen.status_height),
                )
            } else {
                tvStatus.layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                )
            }
        }
    }

    private fun updateStatus(status: StatusInfo) {
        if (status.error != null) {
            tvStatus.setBackgroundColor(
                    activity.resources.getColor(R.color.status_error)
            )
            tvStatus.text = status.status.name + ":" + status.error?.message?:""
        } else {
            tvStatus.setBackgroundColor(
                    activity.resources.getColor(status.status.color())
                    )
            tvStatus.text = status.status.name
        }
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
        }
    }
}
