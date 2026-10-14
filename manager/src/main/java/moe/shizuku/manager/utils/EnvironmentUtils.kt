package moe.shizuku.manager.utils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.SystemProperties
import android.provider.Settings
import moe.shizuku.manager.ShizukuApplication
import moe.shizuku.manager.ShizukuSettings
import com.topjohnwu.superuser.Shell

private val appContext = ShizukuApplication.appContext

object EnvironmentUtils {

    @JvmStatic
    fun isWatch(): Boolean {
        return (appContext.getSystemService(UiModeManager::class.java).currentModeType
                == Configuration.UI_MODE_TYPE_WATCH)
    }

    @JvmStatic
    fun isTelevision(): Boolean {
        return (appContext.getSystemService(UiModeManager::class.java).currentModeType
                == Configuration.UI_MODE_TYPE_TELEVISION ||
                appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
    }

    fun isTlsSupported(): Boolean {
        return if (isTelevision())
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            else Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    fun isWifiRequired(): Boolean {
        return (getAdbTcpPort() <= 0 || !ShizukuSettings.getTcpMode())
    }

    fun isRooted(): Boolean {
        return Shell.getShell().isRoot
    }

    fun getAdbTcpPort(): Int {
        var port = SystemProperties.getInt("service.adb.tcp.port", -1)
        if (port == -1) port = SystemProperties.getInt("persist.adb.tcp.port", -1)
        if (port == -1 && isTelevision() && !isTlsSupported()) port = ShizukuSettings.getTcpPort()
        return port
    }

    /**
     * Returns whether USB debugging is enabled, or null if it cannot be determined.
     *
     * Starting with Android 17, third-party apps can no longer read [Settings.Global.ADB_ENABLED]
     * (it always reads as 0), so a 0 there only means "disabled" on older versions.
     */
    fun isAdbEnabled(): Boolean? {
        val adbEnabled = runCatching {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.ADB_ENABLED, 0)
        }.getOrDefault(0)
        if (adbEnabled > 0) return true
        if (Build.VERSION.SDK_INT < 37) return false

        // adbd only runs (and listens on a TCP port) while debugging is enabled
        if (getAdbTcpPort() > 0) return true
        if (SystemProperties.get("init.svc.adbd") == "running") return true
        return null
    }
}
