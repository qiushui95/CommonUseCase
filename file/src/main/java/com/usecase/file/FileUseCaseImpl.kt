package com.usecase.file

import com.blankj.utilcode.util.EncryptUtils
import java.io.File

public class FileUseCaseImpl : FileUseCase {
    override fun getFileMd5(file: File): String {
        return EncryptUtils.encryptMD5File2String(file).lowercase()
    }

    override fun isEmptyDir(file: File): Boolean {
        if (file.isFile) return false

        val fileList = file.listFiles()?.filter { it.isFile }

        if (fileList.isNullOrEmpty().not()) return false

        val dirList = file.listFiles()?.filter { it.isDirectory }

        if (dirList.isNullOrEmpty()) return true

        for (dir in dirList) {
            if (isEmptyDir(dir).not()) return false
        }

        return true
    }

    override fun cleanEmptyDir(file: File) {
        if (file.isDirectory.not()) return

        file.listFiles()?.forEach { cleanEmptyDir(it) }

        if (isEmptyDir(file)) file.deleteRecursively()
    }
}
