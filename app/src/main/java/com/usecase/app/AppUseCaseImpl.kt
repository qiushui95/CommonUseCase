package com.usecase.app

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.os.storage.StorageManager
import androidx.core.graphics.drawable.toBitmapOrNull
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ShellUtils
import com.blankj.utilcode.util.Utils
import java.io.File

public class AppUseCaseImpl(private val shellRunUseCase: AppShellRunUseCase) : AppUseCase {
    private fun getPkgManager(context: Context): PackageManager {
        return context.packageManager
    }

    private fun getStatsManager(context: Context): StorageStatsManager {
        return context.getSystemService(StorageStatsManager::class.java)
    }

    private fun getStorageManager(context: Context): StorageManager {
        return context.getSystemService(StorageManager::class.java)
    }

    override fun getInstallShell(
        pkgName: String,
        apkList: List<File>,
        obbList: List<ObbFileInfo>,
    ): String {
        val cmdList = mutableListOf<String>()

        cmdList.add("output=$(pm install-create)")
        cmdList.add("sid=$(echo \$output | sed -n 's/.*\\[//;s/\\].*//p')")

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

        return cmdList.joinToString(" && ")
    }

    override fun isSystemApp(context: Context, pkgName: String): Boolean {
        return isSystemApp(getPkgManager(context).getPackageInfo(pkgName, 0))
    }

    override fun isSystemApp(pkgInfo: PackageInfo): Boolean {
        return (ApplicationInfo.FLAG_SYSTEM and (pkgInfo.applicationInfo?.flags ?: -1)) == 1
    }

    override fun killAllUserApp() {
        val cmdList = mutableListOf<String>()

        val application = Utils.getApp()

        val pkgName = application.packageName

        val pkgList = application.packageManager.getInstalledPackages(0)
            .filterNot { lp -> isSystemApp(lp) }
            .filterNot { lp -> lp.packageName == pkgName }
            .map { it.packageName }

        pkgList.mapTo(cmdList) { "dg am stop $it" }
        pkgList.mapTo(cmdList) { "am force-stop $it" }

//        cmdList.add("ps -ef | grep 'u0.*' | grep -v $pkgName | awk '{print $2}' | xargs kill")

        shellRunUseCase.runShell(cmdList.joinToString(";"))
    }

    override fun killApp(pkgName: String) {
        val cmdList = mutableListOf<String>()

        cmdList.add("am force-stop $pkgName")
        cmdList.add("dg am stop $pkgName")

        cmdList.add("ps -ef | grep $pkgName | awk '{print $2}' | xargs kill")

        shellRunUseCase.runShell(cmdList.joinToString(";"))
    }

    override fun launchApp(pkgName: String) {
        val cmd = "dg am start $pkgName"

        shellRunUseCase.runShell(listOf(cmd))
    }

    override fun isAppInstalled(pkgName: String): Boolean {
        return AppUtils.isAppInstalled(pkgName)
    }

    override fun getAppInfo(pkgName: String): AppUtils.AppInfo? {
        if (!isAppInstalled(pkgName)) return null

        return AppUtils.getAppInfo(pkgName)
    }

    override fun getSpaceSize(context: Context, pkgName: String): Long {
        val dir = File(Environment.getDataDirectory(), pkgName)

        val pkgManager = getPkgManager(context)
        val statsManager = getStatsManager(context)
        val storageManager = getStorageManager(context)

        val uid = pkgManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA).uid
        val uuid = storageManager.getUuidForPath(dir)

        return try {
            return statsManager.queryStatsForUid(uuid, uid)
                .run { appBytes + dataBytes + cacheBytes }
        } catch (_: Exception) {
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

    override fun getIconFile(context: Context, pkgName: String): File? = try {
        tryGetIconFile(context, pkgName)
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }

    private fun tryGetIconFile(context: Context, pkgName: String): File? {
        val dir = File(PathUtils.getExternalAppCachePath(), "app_icon_cache")

        val dstFile = File(dir, "$pkgName.png")

        if (dstFile.exists()) {
            val updateTime = getPkgManager(context).getPackageInfo(pkgName, 0).lastUpdateTime

            if (dstFile.lastModified() >= updateTime) return dstFile

            dstFile.delete()
        }

        val applicationInfo = getPkgManager(context).getApplicationInfo(pkgName, 0)

        val bitmap = applicationInfo.loadUnbadgedIcon(context.packageManager)
            ?.toBitmapOrNull() ?: return null

        dir.mkdirs()

        dstFile.createNewFile()

        dstFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        return dstFile
    }

    override fun getApkFileList(context: Context, pkgName: String): List<File> {
        val fileList = mutableListOf<File>()

        val appInfo = getPkgManager(context).getApplicationInfo(pkgName, 0)

        fileList.add(File(appInfo.sourceDir))

        appInfo.splitSourceDirs?.forEach { fileList.add(File(it)) }

        fileList.removeAll { it.exists().not() && it.isFile.not() }

        return fileList
    }

    override fun getObbFileList(pkgName: String): List<File> {
        val sdcardDir = Environment.getExternalStorageDirectory()

        val obbDir = File(sdcardDir, "Android/obb/$pkgName")

        if (obbDir.exists().not()) return emptyList()

        if (obbDir.isDirectory.not()) return emptyList()

        return obbDir.listFiles()?.filter { it.exists() && it.isFile } ?: emptyList()
    }

    override fun chownInternalDir(pkgName: String, uid: String?) {
        uid ?: return

        val cmdList = mutableListOf<String>()

        cmdList.add("chown -R $uid:$uid /data/data/$pkgName")

        ShellUtils.execCmd(cmdList, true, true)
    }
}
