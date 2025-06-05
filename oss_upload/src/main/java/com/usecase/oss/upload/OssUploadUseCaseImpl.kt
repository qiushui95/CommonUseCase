package com.usecase.oss.upload

import android.content.Context
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider
import com.alibaba.sdk.android.oss.model.GetObjectMetaRequest
import com.alibaba.sdk.android.oss.model.ObjectMetadata
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

public abstract class OssUploadUseCaseImpl(private val context: Context) : OssUploadUseCase {
    private companion object {
        const val ENDPOINT = "http://oss-cn-beijing.aliyuncs.com"
    }

    private val ossClient by lazy {
        createOssClient()
    }

    protected abstract fun getAuthServerUrl(): String

    protected abstract suspend fun getBucketName(): String?

    private fun createOssClient(): OSSClient {
        val authServerUrl = getAuthServerUrl()

        val authProvider = OSSAuthCredentialsProvider(authServerUrl)

        val config = ClientConfiguration()

        config.connectionTimeout = 5 * 1000
        config.socketTimeout = 5 * 1000

        return OSSClient(context, ENDPOINT, authProvider, config)
    }

    private suspend fun tryGetBucketName(): String? {
        repeat(5) {
            val bucketName = runCatching { getBucketName() }.getOrNull()

            if (bucketName.isNullOrBlank().not()) return bucketName
        }

        return null
    }

    private suspend fun getFileMeta(
        remotePath: String,
        bucketName: String?,
    ): ObjectMetadata? {
        val bucket = bucketName ?: tryGetBucketName() ?: return null

        val client = createOssClient()

        val request = GetObjectMetaRequest(bucket, remotePath)

        return client.getObjectMeta(request).metadata
    }

    private suspend fun getRemoteFileLength(remotePath: String, bucketName: String?): Long? {
        repeat(5) {
            val result = kotlin.runCatching { getFileMeta(remotePath, bucketName) }

            val metaInfo = result.getOrNull()

            if (metaInfo != null) return metaInfo.contentLength
        }

        return null
    }

    override suspend fun startSyncUpload(
        remotePath: String,
        file: File,
        retryTime: Int,
        progressListener: OssUploadUseCase.OnProgressListener?,
    ): Boolean = withContext(Dispatchers.IO) {
        syncUpload(
            remotePath = remotePath,
            file = file,
            retryTime = retryTime,
            progressListener = progressListener,
        )
    }

    private fun notifySuccessProgress(
        remotePath: String,
        totalSize: Long,
        progressListener: OssUploadUseCase.OnProgressListener?,
    ) {
        progressListener?.onProgress(
            remotePath = remotePath,
            curSize = totalSize,
            totalSize = totalSize,
        )
    }

    private fun notifySuccessProgress(
        remotePath: String,
        file: File,
        progressListener: OssUploadUseCase.OnProgressListener?,
    ) {
        val totalSize = file.length()

        notifySuccessProgress(
            remotePath = remotePath,
            totalSize = totalSize,
            progressListener = progressListener,
        )
    }

    private suspend fun syncUpload(
        remotePath: String,
        file: File,
        retryTime: Int,
        progressListener: OssUploadUseCase.OnProgressListener?,
    ): Boolean {
        val bucketName = tryGetBucketName() ?: return false

        if (getRemoteFileLength(remotePath, bucketName) == file.length()) {
            notifySuccessProgress(
                remotePath = remotePath,
                file = file,
                progressListener = progressListener,
            )
            return true
        }

        val maxTime = retryTime + 1

        repeat(maxTime) {
            val isSuccess = syncUpload(
                bucketName = bucketName,
                remotePath = remotePath,
                file = file,
                curTime = it,
                maxTime = retryTime,
                progressListener = progressListener,
            )

            if (isSuccess) {
                notifySuccessProgress(
                    remotePath = remotePath,
                    file = file,
                    progressListener = progressListener,
                )
                return true
            }
        }

        return false
    }

    private fun syncUpload(
        bucketName: String,
        remotePath: String,
        file: File,
        curTime: Int,
        maxTime: Int,
        progressListener: OssUploadUseCase.OnProgressListener?,
    ): Boolean {
        if (curTime > 0) {
            logMessage("UploadUseCase", "上传失败,正在重试$curTime/$maxTime", file.absolutePath)
        }

        return try {
            syncUpload(
                bucketName = bucketName,
                remotePath = remotePath,
                file = file,
                progressListener = progressListener,
            )

            ossClient.doesObjectExist(bucketName, remotePath)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun syncUpload(
        bucketName: String,
        remotePath: String,
        file: File,
        progressListener: OssUploadUseCase.OnProgressListener?,
    ) {
        val metadata = ObjectMetadata()
        metadata.setHeader("x-oss-object-acl", "public-read")

        val putObjectRequest = PutObjectRequest(bucketName, remotePath, file.absolutePath)

        putObjectRequest.metadata = metadata

        var preNotifyTime = 0L

        putObjectRequest.setProgressCallback { _, currentSize, totalSize ->
            val now = System.currentTimeMillis()

            if (now - preNotifyTime >= 1000 || currentSize == totalSize) {
                progressListener?.onProgress(
                    remotePath = remotePath,
                    curSize = currentSize,
                    totalSize = totalSize,
                )
                preNotifyTime = now
            }
        }

        ossClient.putObject(putObjectRequest)
    }
}
