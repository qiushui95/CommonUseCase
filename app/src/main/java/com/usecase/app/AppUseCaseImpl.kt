package com.usecase.app

import android.app.Application
import android.app.usage.StorageStatsManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.storage.StorageManager
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ShellUtils
import com.blankj.utilcode.util.Utils
import java.io.File

public class AppUseCaseImpl(app: Application) : AppUseCase {
    private val pkgManager by lazy { app.packageManager }
    private val statsManager by lazy { app.getSystemService(StorageStatsManager::class.java) }
    private val storageManager by lazy { app.getSystemService(StorageManager::class.java) }

    override fun runShell(shellList: List<String>, isRoot: Boolean): ShellUtils.CommandResult? {
        return ShellUtils.execCmd(shellList, isRoot, true)
    }

    override fun installApksShell(fileList: List<File>): List<String> {
        val cmdList = mutableListOf<String>()

        cmdList.add("output=\$(pm install-create)")
        cmdList.add("sid=\$(echo \$output | sed -n 's/.*\\[//;s/\\].*//p')")

        for (file in fileList) {
            cmdList.add("pm install-write \$sid ${file.name} ${file.absolutePath}")
        }

        cmdList.add("pm install-commit \$sid")

        return cmdList
    }

    override fun isSystemApp(pkgName: String): Boolean {
        return isSystemApp(pkgManager.getPackageInfo(pkgName, 0))
    }

    override fun isSystemApp(pkgInfo: PackageInfo): Boolean {
        return (ApplicationInfo.FLAG_SYSTEM and pkgInfo.applicationInfo.flags) == 1
    }

    override fun killAllUserApp() {
        val cmdList = mutableListOf<String>()

        val application = Utils.getApp()

        application.packageManager.getInstalledPackages(0)
            .filterNot { lp -> isSystemApp(lp) }
            .filterNot { lp -> lp.packageName == application.packageName }
            .mapTo(cmdList) { lp -> "am force-stop ${lp.packageName}" }

        cmdList.add(
            "ps -ef | grep 'u0.*' | grep -v ${application.packageName} | awk '{print \$2}' | xargs kill",
        )

        runShell(cmdList, true)
    }

    override fun killApp(pkgName: String) {
        val cmdList = mutableListOf<String>()

        cmdList.add("am force-stop $pkgName")

        cmdList.add("ps -ef | grep $pkgName | awk '{print \$2}' | xargs kill")

        runShell(cmdList, true)
    }

    override fun isAppInstalled(pkgName: String): Boolean {
        return AppUtils.isAppInstalled(pkgName)
    }

    override fun getAppInfo(pkgName: String): AppUtils.AppInfo? {
        if (!isAppInstalled(pkgName)) return null

        return AppUtils.getAppInfo(pkgName)
    }

    override fun getSpaceSize(pkgName: String): Long {
        val dir = File(Environment.getDataDirectory(), pkgName)

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
}
