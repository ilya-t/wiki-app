package com.tsourcecode.wiki.app.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tsourcecode.wiki.app.MainActivity

internal fun Context.mainActivityPendingIntent(): PendingIntent {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
