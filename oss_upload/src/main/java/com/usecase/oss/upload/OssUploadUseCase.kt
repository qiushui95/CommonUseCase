package com.usecase.oss.upload

import java.io.File

public interface OssUploadUseCase {
    public fun interface OnProgressListener {
        public fun onProgress(remotePath: String, curSize: Long, totalSize: Long)
    }

    public fun logMessage(vararg message: String)

    public suspend fun startSyncUpload(
        remotePath: String,
        file: File,
        retryTime: Int = 2,
        progressListener: OnProgressListener? = null,
    ): Boolean
}
