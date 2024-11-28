package com.usecase.app

import android.app.Application
import android.app.usage.StorageStatsManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.os.storage.StorageManager
import androidx.core.graphics.drawable.toBitmapOrNull
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ShellUtils
import com.blankj.utilcode.util.Utils
import java.io.File

public class AppUseCaseImpl : AppUseCase {
    private fun getPkgManager(app: Application): PackageManager {
        return app.packageManager
    }

    private fun getStatsManager(app: Application): StorageStatsManager {
        return app.getSystemService(StorageStatsManager::class.java)
    }

    private fun getStorageManager(app: Application): StorageManager {
        return app.getSystemService(StorageManager::class.java)
    }

    override fun runShell(shellList: List<String>, isRoot: Boolean): ShellUtils.CommandResult? {
        return ShellUtils.execCmd(shellList, isRoot, true)
    }

    override fun getInstallShell(
        pkgName: String,
        apkList: List<File>,
        obbList: List<ObbFileInfo>,
    ): List<String> {
        val cmdList = mutableListOf<String>()

        cmdList.add("output=\$(pm install-create)")
        cmdList.add("sid=\$(echo \$output | sed -n 's/.*\\[//;s/\\].*//p')")

        for (file in apkList) {
            cmdList.add("pm install-write \$sid ${file.name} ${file.absolutePath}")
        }

        cmdList.add("pm install-commit \$sid")

        if (obbList.isNotEmpty()) {
            cmdList.add("mkdir -p /sdcard/Android/obb/$pkgName")
            obbList.mapTo(cmdList) {
                "cp ${it.srcFile.absolutePath} /sdcard/Android/obb/$pkgName/${it.fileName}"
            }
        }

        return cmdList
    }

    override fun isSystemApp(app: Application, pkgName: String): Boolean {
        return isSystemApp(getPkgManager(app).getPackageInfo(pkgName, 0))
    }

    override fun isSystemApp(pkgInfo: PackageInfo): Boolean {
        return (ApplicationInfo.FLAG_SYSTEM and (pkgInfo.applicationInfo?.flags ?: -1)) == 1
    }

    override fun killAllUserApp() {
        val cmdList = mutableListOf<String>()

        val application = Utils.getApp()

        val pkgName = application.packageName

        application.packageManager.getInstalledPackages(0)
            .filterNot { lp -> isSystemApp(lp) }
            .filterNot { lp -> lp.packageName == pkgName }
            .mapTo(cmdList) { lp -> "am force-stop ${lp.packageName}" }

        cmdList.add("ps -ef | grep 'u0.*' | grep -v $pkgName | awk '{print \$2}' | xargs kill")

        runShell(cmdList, true)
    }

    override fun killApp(pkgName: String) {
        val cmdList = mutableListOf<String>()

        cmdList.add("am force-stop $pkgName")

        cmdList.add("ps -ef | grep $pkgName | awk '{print \$2}' | xargs kill")

        runShell(cmdList, true)
    }

    override fun launchApp(pkgName: String) {
        val pkgManager = getPkgManager(Utils.getApp())

        val intent = pkgManager.getLaunchIntentForPackage(pkgName) ?: return

        ActivityUtils.startActivity(intent)
    }

    override fun isAppInstalled(pkgName: String): Boolean {
        return AppUtils.isAppInstalled(pkgName)
    }

    override fun getAppInfo(pkgName: String): AppUtils.AppInfo? {
        if (!isAppInstalled(pkgName)) return null

        return AppUtils.getAppInfo(pkgName)
    }

    override fun getSpaceSize(app: Application, pkgName: String): Long {
        val dir = File(Environment.getDataDirectory(), pkgName)

        val pkgManager = getPkgManager(app)
        val statsManager = getStatsManager(app)
        val storageManager = getStorageManager(app)

        val uid = pkgManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA).uid
        val uuid = storageManager.getUuidForPath(dir)

        return try {
            return statsManager.queryStatsForUid(uuid, uid)
                .run { appBytes + dataBytes + cacheBytes }
        } catch (e: Exception) {
            0
        }
    }

    override fun getAppSignature(pkgName: String): String? {
        return getAppSignature(AppUtils.getAppInfo(pkgName))
    }

    override fun getAppSignature(appInfo: AppUtils.AppInfo?): String? {
        appInfo ?: return null

        return AppUtils.getAppSignaturesMD5(appInfo.packageName)
            .firstOrNull()?.replace(":", "")
    }

    override fun getUID(pkgName: String): String? {
        if (isAppInstalled(pkgName).not()) return null

        val packageManager = Utils.getApp().packageManager

        val uid = packageManager.runCatching {
            getApplicationInfo(pkgName, 0).uid
        }.getOrNull() ?: return null

        if (uid == 1000) return "system"

        return "u0_a${uid - 10000}"
    }

    override fun getIconFile(app: Application, pkgName: String): File? {
        val dir = File(PathUtils.getExternalAppCachePath(), "app_icon_cache")

        val dstFile = File(dir, "$pkgName.png")

        if (dstFile.exists()) {
            val updateTime = getPkgManager(app).getPackageInfo(pkgName, 0).lastUpdateTime

            if (dstFile.lastModified() >= updateTime) return dstFile

            dstFile.delete()
        }

        val applicationInfo = getPkgManager(app).getApplicationInfo(pkgName, 0)

        val bitmap = applicationInfo.loadUnbadgedIcon(app.packageManager)
            ?.toBitmapOrNull() ?: return null

        dir.mkdirs()

        dstFile.createNewFile()

        dstFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        return dstFile
    }
}
