package com.usecase.app

import android.content.Context
import android.content.pm.PackageInfo
import com.blankj.utilcode.util.AppUtils
import java.io.File

public interface AppUseCase {
    public fun getInstallShell(
        pkgName: String,
        apkList: List<File>,
        obbList: List<ObbFileInfo>,
    ): String

    public fun isSystemApp(context: Context, pkgName: String): Boolean

    public fun isSystemApp(pkgInfo: PackageInfo): Boolean

    public fun killAllUserApp()

    public fun killApp(pkgName: String)

    public fun launchApp(pkgName: String)

    public fun isAppInstalled(pkgName: String): Boolean

    public fun getAppInfo(pkgName: String): AppUtils.AppInfo?

    public fun getSpaceSize(context: Context, pkgName: String): Long

    public fun getAppSignature(pkgName: String): String?

    public fun getAppSignature(appInfo: AppUtils.AppInfo?): String?

    public fun getUID(pkgName: String): String?

    public fun getIconFile(context: Context, pkgName: String): File?

    public fun getApkFileList(context: Context, pkgName: String): List<File>

    public fun getObbFileList(pkgName: String): List<File>

    public fun chownInternalDir(pkgName: String, uid: String? = getUID(pkgName))
}
