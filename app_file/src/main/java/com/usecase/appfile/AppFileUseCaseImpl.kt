package com.usecase.appfile

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import com.blankj.utilcode.util.ShellUtils
import java.io.File

public abstract class AppFileUseCaseImpl : AppFileUseCase {
    private fun getPkgManager(app: Application): PackageManager {
        return app.packageManager
    }

    protected abstract val dstDir: File

    protected abstract fun isAppInstalled(pkgName: String): Boolean

    override fun getIconFile(): File {
        return File(dstDir, "icon.png")
    }

    override fun copyIcon(app: Application, pkgName: String, dstFile: File) {
        val applicationInfo = getPkgManager(app).getApplicationInfo(pkgName, 0)

        val bitmap = applicationInfo.loadUnbadgedIcon(app.packageManager)
            ?.toBitmapOrNull() ?: return

        try {
            dstFile.parentFile?.mkdirs()

            dstFile.delete()
            dstFile.createNewFile()
            dstFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
    }

    override fun getExternalDataFile(): File {
        return File(dstDir, "external_data.tar.external")
    }

    override fun tarExternalData(pkgName: String, dstFile: File) {
        val cmdList = mutableListOf<String>()

        cmdList.add("mkdir -p ${dstFile.parentFile?.absolutePath}")

        cmdList.add("cd / && tar -cf ${dstFile.absolutePath} /sdcard/Android/data/$pkgName")

        ShellUtils.execCmd(cmdList, true, true)
    }

    override fun getInternalDataFile(): File {
        return File(dstDir, "internal_data.tar.internal")
    }

    override fun tarInternalData(pkgName: String, dstFile: File) {
        val cmdList = mutableListOf<String>()

        cmdList.add("mkdir -p ${dstFile.parentFile?.absolutePath}")

        cmdList.add("cd / && tar -cf ${dstFile.absolutePath} /data/data/$pkgName")

        ShellUtils.execCmd(cmdList, true, false)
    }

    override fun getCustomDirFile(): File {
        return File(dstDir, "custom_dir.tar.custom")
    }

    override fun tarCustomDir(dirList: List<String>, dstFile: File) {
        if (dirList.isEmpty()) return

        val cmdList = mutableListOf<String>()

        cmdList.add("mkdir -p ${dstFile.parentFile?.absolutePath}")

        val tarCmdList = mutableListOf<String>()

        tarCmdList.add("tar")
        tarCmdList.add("-cf")
        tarCmdList.add(dstFile.absolutePath)

        dirList.forEach { tarCmdList.add(it) }

        val tarCmd = tarCmdList.joinToString(" ")

        cmdList.add("cd / && $tarCmd")

        ShellUtils.execCmd(cmdList, true, false)
    }

    override fun getApkFileList(app: Application, pkgName: String): List<File> {
        val fileList = mutableListOf<File>()

        val appInfo = getPkgManager(app).getApplicationInfo(pkgName, 0)

        fileList.add(File(appInfo.sourceDir))

        appInfo.splitSourceDirs?.forEach { fileList.add(File(it)) }

        fileList.removeAll { it.exists().not() && it.isFile.not() }

        return fileList
    }

    public override fun getObbFileList(pkgName: String): List<File> {
        val obbDir = File("/sdcard/Android/obb", pkgName)

        if (obbDir.exists().not()) return emptyList()

        if (obbDir.isDirectory.not()) return emptyList()

        return obbDir.listFiles()?.filter { it.exists() && it.isFile } ?: emptyList()
    }

    override fun checkTarFile(file: File): Boolean {
        return when {
            file.exists().not() -> false
            file.isFile.not() -> false
            else -> true
        }
    }
}
