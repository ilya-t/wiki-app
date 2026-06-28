package com.tsourcecode.wiki.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tsourcecode.wiki.app.bottombar.BottomBarController
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.ScreenFactory

class ActivityComponent(
        private val activity: AppCompatActivity,
        private val appComponent: AppComponent,
) {
    private val domain = appComponent.domain
    private val screenFactory = ScreenFactory(
            activity,
            appComponent.domain,
    )
    private val navigator = ActivityNavigator(
            activity,
            appComponent.domain.navigator,
            screenFactory,
            domain.quickStatusController,
    )

    private val bottomBarView = BottomBarController(
            activity,
            domain.navigator,
            domain.projectComponentResolver,
            rootView = activity.findViewById(R.id.control_bar),
    )

    private val quickStateController = QuickStatusViewModel(
            activity,
            appComponent.quickStatusController)

    fun dispatchBackPressed(): Boolean {
        if (appComponent.domain.navigator.goBack()) {
            return true
        }

        return false
    }

    init {
        requestStoragePermissions()
        requestNotificationPermissionIfNeeded()
    }

    private fun requestStoragePermissions() {
        val externalStorageAccess = appComponent.domain.platformDeps.externalStorageAccess
        externalStorageAccess.bind(activity)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                externalStorageAccess.bind(null)
            }
        })
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST,
                )
            }
        }
    }
}

private const val NOTIFICATION_PERMISSION_REQUEST = 1
