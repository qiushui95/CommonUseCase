package com.usecase.appfile

import android.app.Application
import java.io.File

public interface AppFileUseCase {
    public fun getIconFile(): File

    public fun copyIcon(app: Application, pkgName: String)

    public fun getExternalDataFile(): File

    public fun tarExternalData(pkgName: String)

    public fun getInternalDataFile(): File

    public fun tarInternalData(pkgName: String)

    public fun getCustomDirFile(): File

    public fun tarCustomDir(dirList: List<String>)

    public fun getApkFileList(app: Application, pkgName: String): List<File>

    public fun getObbFileList(pkgName: String): List<File>

    public fun checkTarFile(file: File): Boolean
}
