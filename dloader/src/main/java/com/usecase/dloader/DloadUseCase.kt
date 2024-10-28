package com.usecase.dloader

import java.io.File

public interface DloadUseCase {
    public fun interface OnProgressListener {
        public fun onProgress(dloadUrl: String, curSize: Long, totalSize: Long)
    }

    /**
     * 开始同步下载
     * @param url 下载地址
     * @param dstFile 保存到的文件
     * @param retryTime 重试次数
     * @param progressListener 下载进度监听
     */
    public suspend fun startSyncDload(
        url: String,
        dstFile: File,
        retryTime: Int = 2,
        progressListener: OnProgressListener? = null,
    ): Boolean

    /**
     * 获取保存到文件(在下载目录)
     * @param url 下载地址
     */
    public fun getDstFileInDownloadDir(url: String): File
}
