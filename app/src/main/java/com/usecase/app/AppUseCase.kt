package com.usecase.app

import android.app.Application
import android.content.pm.PackageInfo
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ShellUtils
import java.io.File

public interface AppUseCase {
    public fun runShell(shellList: List<String>, isRoot: Boolean): ShellUtils.CommandResult?

    public fun getInstallShell(
        pkgName: String,
        apkList: List<File>,
        obbList: List<ObbFileInfo>,
    ): List<String>

    public fun isSystemApp(app: Application, pkgName: String): Boolean

    public fun isSystemApp(pkgInfo: PackageInfo): Boolean

    public fun killAllUserApp()

    public fun killApp(pkgName: String)

    public fun isAppInstalled(pkgName: String): Boolean

    public fun getAppInfo(pkgName: String): AppUtils.AppInfo?

    public fun getSpaceSize(app: Application, pkgName: String): Long

    public fun getAppSignature(pkgName: String): String?

    public fun getAppSignature(appInfo: AppUtils.AppInfo?): String?

    public fun getUID(pkgName: String): String?
}
