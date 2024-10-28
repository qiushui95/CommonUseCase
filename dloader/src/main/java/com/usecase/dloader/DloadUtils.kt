package com.usecase.dloader

import com.blankj.utilcode.util.EncryptUtils
import com.blankj.utilcode.util.PathUtils
import okhttp3.Response
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal const val HEADER_ACCEPT_RANGE = "Accept-Ranges"

internal const val HEADER_ACCEPT_RANGE_LEGACY = "accept-ranges"

internal const val HEADER_ACCEPT_RANGE_COMPAT = "AcceptRanges"

internal const val HEADER_CONTENT_LENGTH = "content-length"

internal const val HEADER_CONTENT_LENGTH_LEGACY = "Content-Length"

internal const val HEADER_CONTENT_LENGTH_COMPAT = "ContentLength"

internal const val HEADER_TRANSFER_ENCODING = "Transfer-Encoding"

internal const val HEADER_TRANSFER_LEGACY = "transfer-encoding"

internal const val HEADER_TRANSFER_ENCODING_COMPAT = "TransferEncoding"

internal const val HEADER_CONTENT_RANGE = "Content-Range"

internal const val HEADER_CONTENT_RANGE_LEGACY = "content-range"

internal const val HEADER_CONTENT_RANGE_COMPAT = "ContentRange"

internal object DloadUtils {
    private fun getHeaderValue(
        response: Response,
        vararg keys: String,
    ): String? {
        for (key in keys) {
            val value = response.headers[key]
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return null
    }

    fun getContentLengthFromHeader(response: Response): Long? {
        if (response.isSuccessful.not()) return null

        val contentRange = getHeaderValue(
            response,
            HEADER_CONTENT_RANGE,
            HEADER_CONTENT_RANGE_LEGACY,
            HEADER_CONTENT_RANGE_COMPAT,
        )
        val lastIndexOf = contentRange?.lastIndexOf("/")
        var contentLength: Long? = null

        if (lastIndexOf != null && lastIndexOf != -1 && lastIndexOf < contentRange.length) {
            contentLength = contentRange.substring(lastIndexOf + 1).toLongOrNull()
        }

        if (contentLength == null) {
            contentLength = getHeaderValue(
                response,
                HEADER_CONTENT_LENGTH,
                HEADER_CONTENT_LENGTH_LEGACY,
                HEADER_CONTENT_LENGTH_COMPAT,
            )?.toLongOrNull()
        }
        return contentLength
    }

    fun acceptRanges(response: Response): Boolean {
        getContentLengthFromHeader(response) ?: return false

        val acceptRangeValue = getHeaderValue(
            response,
            HEADER_ACCEPT_RANGE,
            HEADER_ACCEPT_RANGE_LEGACY,
            HEADER_ACCEPT_RANGE_COMPAT,
        )
        val transferValue = getHeaderValue(
            response,
            HEADER_TRANSFER_ENCODING,
            HEADER_TRANSFER_LEGACY,
            HEADER_TRANSFER_ENCODING_COMPAT,
        )
        val acceptsRanges =
            response.code == HttpURLConnection.HTTP_PARTIAL || acceptRangeValue == "bytes"

        return acceptsRanges || transferValue?.lowercase() != "chunked"
    }

    private fun getFileNameFromUrl(url: String): String {
        // 获取路径部分
        var path = URL(url).path

        val defaultName by lazy { EncryptUtils.encryptMD5ToString(url) }

        // 如果路径为空，返回默认文件名
        if (path.isEmpty()) return defaultName

        // 去掉路径末尾的斜杠（如果有）
        if (path.endsWith("/")) {
            path = path.removeSuffix("/")
        }

        // 获取路径最后的部分作为文件名
        var fileName = path.substringAfterLast("/")

        // 检查文件名是否为空
        if (fileName.isEmpty()) return defaultName

        // 处理查询参数（如果有），去除可能的 `?` 或 `#` 后缀
        fileName = fileName.substringBefore("?").substringBefore("#")

        // 如果依旧为空，返回默认文件名
        return fileName.ifEmpty { defaultName }
    }

    fun getDstFile(url: String): File {
        val fileName = getFileNameFromUrl(url)

        return File(PathUtils.getExternalDownloadsPath(), fileName)
    }
}
