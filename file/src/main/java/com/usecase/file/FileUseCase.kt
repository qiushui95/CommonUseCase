package com.usecase.file

import java.io.File

public interface FileUseCase {
    public fun getFileMd5(file: File): String

    public fun getRandomFileMd5(file: File): String

    public fun isEmptyDir(file: File): Boolean

    public fun cleanEmptyDir(file: File)
}
