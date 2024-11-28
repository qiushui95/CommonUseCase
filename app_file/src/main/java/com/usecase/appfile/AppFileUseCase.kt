package com.usecase.appfile

import android.app.Application
import java.io.File

public interface AppFileUseCase {
    public fun getExternalDataFile(): File

    public fun tarExternalData(pkgName: String, dstFile: File = getExternalDataFile())

    public fun getInternalDataFile(): File

    public fun tarInternalData(pkgName: String, dstFile: File = getInternalDataFile())

    public fun getCustomDirFile(): File

    public fun tarCustomDir(dirList: List<String>, dstFile: File = getCustomDirFile())

    public fun getApkFileList(app: Application, pkgName: String): List<File>

    public fun getObbFileList(pkgName: String): List<File>

    public fun checkTarFile(file: File): Boolean
}
