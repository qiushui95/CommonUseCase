package com.usecase.appfile

import java.io.File

public interface AppFileUseCase {
    public fun getExternalDataFile(): File

    public fun tarExternalData(pkgName: String, dstFile: File = getExternalDataFile())

    public fun getInternalDataFile(): File

    public fun tarInternalData(pkgName: String, dstFile: File = getInternalDataFile())

    public fun getCustomDirFile(): File

    public fun tarCustomDir(dirList: List<String>, dstFile: File = getCustomDirFile())

    public fun checkTarFile(file: File): Boolean
}
