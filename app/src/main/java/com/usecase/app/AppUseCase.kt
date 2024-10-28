package com.usecase.app

import android.content.pm.PackageInfo
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ShellUtils
import java.io.File

public interface AppUseCase {
    public fun runShell(shellList: List<String>, isRoot: Boolean): ShellUtils.CommandResult?

    public fun installApksShell(fileList: List<File>): List<String>

    public fun isSystemApp(pkgName: String): Boolean

    public fun isSystemApp(pkgInfo: PackageInfo): Boolean

    public fun killAllUserApp()

    public fun killApp(pkgName: String)

    public fun isAppInstalled(pkgName: String): Boolean

    public fun getAppInfo(pkgName: String): AppUtils.AppInfo?

    public fun getSpaceSize(pkgName: String): Long

    public fun getAppSignature(pkgName: String): String

    public fun getAppSignature(appInfo: AppUtils.AppInfo?): String

    public fun getUID(pkgName: String): String?
}
