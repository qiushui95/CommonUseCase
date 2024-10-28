package com.usecase.dloader

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import java.io.File

private const val SPLIT_SIZE = 10L * 1024 * 1024
private const val SPLIT_TIMEOUT_SECOND = 15

public abstract class DloadUseCaseImpl : DloadUseCase {
    private data class DownInfo(
        val url: String,
        val dstFile: File,
        val retryTime: Int,
        val progressListener: DloadUseCase.OnProgressListener?,
    )

    private data class HeaderInfo(
        val contentLength: Long,
        val canSplit: Boolean,
    )

    private class ProgressUseCase(
        val scope: CoroutineScope,
        val downUrl: String,
        val listener: DloadUseCase.OnProgressListener?,
        val totalSize: Long,
    ) {
        private val dispatcher by lazy { Dispatchers.Default.limitedParallelism(1) }

        private val notifyJob = SupervisorJob()

        private var curSize = 0L

        fun startNotify() = scope.launch(dispatcher) {
            if (notifyJob.children.any { it.isActive }) return@launch
            launch(notifyJob) {
                while (scope.isActive) {
                    delay(1000)
                    notifyProgress()
                }
            }
        }

        private fun notifyProgress() {
            listener?.onProgress(downUrl, curSize.coerceAtMost(totalSize - 1), totalSize)
        }

        fun endNotify() {
            notifyJob.cancel()
            notifyProgress()
        }

        fun notifySuccess() {
            listener?.onProgress(downUrl, totalSize, totalSize)
        }

        fun plusSize(size: Long) = scope.launch(dispatcher) {
            curSize += size
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .build()
    }

    protected abstract fun logMessage(message: String)

    private val subTaskProcessors by lazy {
        Runtime.getRuntime().availableProcessors() - 2
    }

    private val errorHandler by lazy {
        CoroutineExceptionHandler { _, _ -> }
    }

    private val subTaskDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(subTaskProcessors) +
            errorHandler
    }

    public override suspend fun startSyncDload(
        url: String,
        dstFile: File,
        retryTime: Int,
        progressListener: DloadUseCase.OnProgressListener?,
    ): Boolean = withContext(Dispatchers.IO) {
        syncDload(
            scope = this,
            downInfo = DownInfo(
                url = url,
                dstFile = dstFile,
                retryTime = retryTime,
                progressListener = progressListener,
            ),
        )
    }

    override fun getDstFileInDownloadDir(url: String): File {
        return DloadUtils.getDstFile(url)
    }

    private fun deleteFile(file: File) {
        file.deleteRecursively()
    }

    private suspend fun syncDload(
        scope: CoroutineScope,
        downInfo: DownInfo,
    ): Boolean {
        val maxTime = downInfo.retryTime + 1

        repeat(maxTime) {
            if (syncDload(scope, downInfo, it, maxTime)) {
                return true
            }
        }

        return false
    }

    private suspend fun syncDload(
        scope: CoroutineScope,
        downInfo: DownInfo,
        curTime: Int,
        maxTime: Int,
    ): Boolean {
        if (curTime > 0) {
            logMessage("下载失败,正在重试$curTime/$maxTime")
        }

        try {
            return startSyncDload(scope = scope, downInfo = downInfo) && downInfo.dstFile.exists()
        } catch (e: Exception) {
            e.printStackTrace()
            deleteFile(downInfo.dstFile)
            return false
        }
    }

    private fun getSubTaskNum(totalLength: Long, splitSize: Long): Int {
        if (totalLength < splitSize) return 1

        val num = (totalLength / splitSize).toInt()

        if (totalLength % splitSize == 0L) return num

        return num + 1
    }

    private fun getSubTaskDir(dstFile: File): File {
        val dir = dstFile.parentFile?.absolutePath
        val name = dstFile.name

        val result = File(dir, ".${name}_tmp")

        result.mkdirs()

        return result
    }

    private fun getSubTaskFile(
        dstFile: File,
        index: Int,
        dir: File = getSubTaskDir(dstFile),
    ): File {
        val name = dstFile.name + ".$index"

        return File(dir, name)
    }

    private fun getHeaderInfo(response: okhttp3.Response): HeaderInfo? {
        val contentLength = DloadUtils.getContentLengthFromHeader(
            response,
        ) ?: return null

        val canSplit = DloadUtils.acceptRanges(response)

        return HeaderInfo(contentLength, canSplit)
    }

    private fun getHeaderInfoFromResponse(response: okhttp3.Response): HeaderInfo? = try {
        getHeaderInfo(response)
    } catch (ex: Exception) {
        null
    } finally {
        response.body?.close()
    }

//    private fun getHeadHeaderInfo(url: String): HeaderInfo? {
//        val request = okhttp3.Request.Builder()
//            .url(url)
//            .head()
//            .build()
//
//        return getHeaderInfoFromResponse(okHttpClient.newCall(request).execute())
//    }

    private fun getGetHeaderInfo(url: String): HeaderInfo? {
        val request = okhttp3.Request.Builder()
            .url(url)
            .get()
            .header("Range", "0-")
            .build()

        return getHeaderInfoFromResponse(okHttpClient.newCall(request).execute())
    }

    private fun getHeaderInfo(url: String): HeaderInfo? {
        return getGetHeaderInfo(url)
    }

    private suspend fun startSyncDload(scope: CoroutineScope, downInfo: DownInfo): Boolean {
        val headerInfo = getHeaderInfo(downInfo.url) ?: return false

        val dstFile = downInfo.dstFile

        val (contentLength, canSplit) = headerInfo

        if (dstFile.exists() && dstFile.length() == contentLength) return true

        val splitSize = if (canSplit) SPLIT_SIZE else contentLength

        val useCase = ProgressUseCase(
            scope = scope,
            downUrl = downInfo.url,
            listener = downInfo.progressListener,
            totalSize = contentLength,
        )

        val subTaskNum = getSubTaskNum(contentLength, splitSize)

        val subFileDir = getSubTaskDir(dstFile)

        val subFileList = (0..<subTaskNum).map { getSubTaskFile(dstFile, it, subFileDir) }

        val timeoutSecond = if (canSplit) SPLIT_TIMEOUT_SECOND else Int.MAX_VALUE

        val jobList = subFileList.mapIndexed { index, file ->
            startSubTask(
                progressUseCase = useCase,
                eachSize = splitSize,
                dstFile = file,
                index = index,
                timeoutSecond = timeoutSecond,
            )
        }

        useCase.startNotify()

        jobList.forEach { it.join() }

        useCase.endNotify()

        mergeFile(subFileList, dstFile, useCase.totalSize)

        val isSuccess = when {
            dstFile.exists().not() -> false
            dstFile.length() != useCase.totalSize -> false
            else -> true
        }

        if (isSuccess) {
            deleteFile(subFileDir)
            useCase.notifySuccess()
        } else {
            deleteFile(dstFile)
        }

        return isSuccess
    }

    private fun startSubTask(
        progressUseCase: ProgressUseCase,
        eachSize: Long,
        dstFile: File,
        index: Int,
        timeoutSecond: Int,
    ) = progressUseCase.scope.launch(subTaskDispatcher + SupervisorJob()) {
        val startIndex = eachSize * index

        if (startIndex >= progressUseCase.totalSize) return@launch

        val endIndex = (startIndex + eachSize - 1).coerceAtMost(progressUseCase.totalSize - 1)

        val totalSize = endIndex - startIndex + 1

        if (dstFile.exists() && dstFile.length() == totalSize) {
            progressUseCase.plusSize(totalSize)
        } else {
            dloadSubFile(progressUseCase, dstFile, startIndex, endIndex, totalSize, timeoutSecond)
        }
    }

    private suspend fun dloadSubFile(
        progressUseCase: ProgressUseCase,
        splitFile: File,
        startIndex: Long,
        endIndex: Long,
        totalSize: Long,
        timeoutSecond: Int,
    ) = withTimeout(timeoutSecond * 1000L) {
        dloadSubFile(progressUseCase, splitFile, startIndex, endIndex, totalSize, this)
    }

    private fun dloadSubFile(
        progressUseCase: ProgressUseCase,
        splitFile: File,
        startIndex: Long,
        endIndex: Long,
        totalSize: Long,
        scope: CoroutineScope,
    ) {
        deleteFile(splitFile)
        splitFile.createNewFile()

        val request = okhttp3.Request.Builder()
            .url(progressUseCase.downUrl)
            .get()
            .header("Range", "bytes=$startIndex-$endIndex")
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (response.isSuccessful.not()) return

        response.body?.use { body ->
            body.byteStream().use { input ->
                splitFile.outputStream().use { output ->
                    val buffer = ByteArray(1024 * 126)

                    while (scope.isActive) {
                        val length = input.read(buffer)

                        if (length == -1) break

                        output.write(buffer, 0, length)

                        progressUseCase.plusSize(length * 1L)
                    }
                }
            }
        }

        if (splitFile.length() != totalSize) splitFile.delete()
    }

    private fun mergeFile(subFileList: List<File>, dstFile: File, totalLength: Long) {
        val splitLength = subFileList.sumOf { it.length() }

        if (splitLength != totalLength) return

        deleteFile(dstFile)
        dstFile.createNewFile()

        dstFile.outputStream().use { output ->
            for (subFile in subFileList) {
                subFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        }
    }
}
