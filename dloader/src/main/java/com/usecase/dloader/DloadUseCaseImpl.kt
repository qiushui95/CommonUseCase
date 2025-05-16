package com.usecase.dloader

import android.util.Log
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
import java.io.FileOutputStream

private const val SPLIT_SIZE = 10L * 1024 * 1024
private const val SPLIT_TIMEOUT_SECOND = 5
private const val SPLIT_TIMEOUT_SECOND_DELTA = 10

public class DloadUseCaseImpl : DloadUseCase {
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

    private data class SubTaskInfo(
        val index: Int,
        val startIndex: Long,
        val endIndex: Long,
        val splitFile: File,
    ) {
        val totalSize: Long = endIndex - startIndex + 1

        val httpStartIndex: Long = startIndex + splitFile.length()

        val isSuccess: Boolean
            get() = splitFile.exists() && splitFile.length() == totalSize
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .build()
    }

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

    override fun logMessage(message: String) {
        Log.e("DloadUseCase", message)
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
        val maxTimes = downInfo.retryTime + 1

        repeat(maxTimes) {
            if (syncDload(scope, downInfo, it, maxTimes)) {
                return true
            }
        }

        return false
    }

    private suspend fun syncDload(
        scope: CoroutineScope,
        downInfo: DownInfo,
        curTimes: Int,
        maxTimes: Int,
    ): Boolean {
        if (curTimes > 0) {
            logMessage("下载失败,正在重试$curTimes/$maxTimes")
        }

        try {
            return startSyncDload(scope, downInfo, curTimes) && downInfo.dstFile.exists()
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

    private fun getTimeoutSecond(canSplit: Boolean, curTimes: Int): Int {
        if (canSplit.not()) return Int.MAX_VALUE

        return SPLIT_TIMEOUT_SECOND + curTimes * SPLIT_TIMEOUT_SECOND_DELTA
    }

    private fun getSubTaskInfo(
        index: Int,
        splitSize: Long,
        totalSize: Long,
        dstFile: File,
        subFileDir: File,
    ): SubTaskInfo? {
        val startIndex = splitSize * index

        if (startIndex >= totalSize) return null

        val endIndex = (startIndex + splitSize - 1).coerceAtMost(totalSize - 1)

        val splitFile = File(subFileDir, dstFile.name + ".$index")

        return SubTaskInfo(
            index = index,
            startIndex = startIndex,
            endIndex = endIndex,
            splitFile = splitFile,
        )
    }

    private suspend fun startSyncDload(
        scope: CoroutineScope,
        downInfo: DownInfo,
        curTimes: Int,
    ): Boolean {
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

        val subTaskList = (0..<subTaskNum).mapNotNull {
            getSubTaskInfo(
                index = it,
                splitSize = splitSize,
                totalSize = useCase.totalSize,
                dstFile = dstFile,
                subFileDir = subFileDir,
            )
        }

        for (taskInfo in subTaskList) {
            useCase.plusSize(taskInfo.splitFile.length())
        }

        val timeoutSecond = getTimeoutSecond(canSplit, curTimes)

        val jobList = subTaskList.map {
            startSubTask(
                progressUseCase = useCase,
                timeoutSecond = timeoutSecond,
                subTaskInfo = it,
            )
        }

        useCase.startNotify()

        jobList.forEach { it.join() }

        useCase.endNotify()

        mergeFile(subTaskList, dstFile)

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
        timeoutSecond: Int,
        subTaskInfo: SubTaskInfo,
    ) = progressUseCase.scope.launch(subTaskDispatcher + SupervisorJob()) {
        if (subTaskInfo.isSuccess) return@launch

        dloadSubFile(progressUseCase, timeoutSecond, subTaskInfo)
    }

    private suspend fun dloadSubFile(
        progressUseCase: ProgressUseCase,
        timeoutSecond: Int,
        subTaskInfo: SubTaskInfo,
    ) = withTimeout(timeoutSecond * 1000L) {
        dloadSubFile(
            progressUseCase = progressUseCase,
            scope = this,
            subTaskInfo = subTaskInfo,
        )
    }

    private fun dloadSubFile(
        progressUseCase: ProgressUseCase,
        scope: CoroutineScope,
        subTaskInfo: SubTaskInfo,
    ) {
        val splitFile = subTaskInfo.splitFile

        if (splitFile.exists().not()) {
            splitFile.createNewFile()
        }

        val request = okhttp3.Request.Builder()
            .url(progressUseCase.downUrl)
            .get()
            .header("Range", "bytes=${subTaskInfo.httpStartIndex}-${subTaskInfo.endIndex}")
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (response.isSuccessful.not()) return

        response.body?.use { body ->
            body.byteStream().use { input ->
                FileOutputStream(splitFile, true).use { output ->

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
    }

    private fun mergeFile(subTaskList: List<SubTaskInfo>, dstFile: File) {
        if (subTaskList.any { it.isSuccess.not() }) return

        deleteFile(dstFile)
        dstFile.createNewFile()

        dstFile.outputStream().use { output ->
            for (subTaskInfo in subTaskList) {
                subTaskInfo.splitFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        }
    }
}
