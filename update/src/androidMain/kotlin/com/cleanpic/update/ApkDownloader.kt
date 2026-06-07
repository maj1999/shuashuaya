package com.cleanpic.update

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.io.readByteArray
import java.io.File

private const val DOWNLOAD_BUFFER = 8192L

/**
 * App 内 ktor 流式下载（取代系统 DownloadManager）。
 *
 * 根因：DownloadManager 是独立系统进程，梯子等环境下与 App 网络栈不一致——能"检查到更新"却"下载 0% 卡死"。
 * 改用与检查更新同一条 HttpClient/网络通道下载，保证"能检查就能下"。
 *
 * 选源竞速（[selectFastestSource]）挑出当前网络最快可达的源优先下载，其余作失败兜底；
 * 每个源下载完成后用 [ApkIntegrity] 校验，挡住防盗链/风控返回的 HTML 伪装包。
 *
 * @return 任一源成功下载且校验通过返回 true；全部失败返回 false。
 */
internal suspend fun downloadApk(
    client: HttpClient,
    candidates: List<String>,
    destFile: File,
    expectedSha256: String,
    expectedSize: Long,
    onProgress: (Float) -> Unit = {},
): Boolean {
    if (candidates.isEmpty()) return false
    val winner = selectFastestSource(client, candidates)
    // 竞速胜出者优先，其余按原序兜底（胜出者若中途失败仍可回退）
    val order = if (winner != null) listOf(winner) + candidates.filter { it != winner } else candidates
    for (url in order) {
        if (downloadOnce(client, url, destFile, expectedSha256, expectedSize, onProgress)) return true
    }
    return false
}

private suspend fun downloadOnce(
    client: HttpClient,
    url: String,
    destFile: File,
    expectedSha256: String,
    expectedSize: Long,
    onProgress: (Float) -> Unit,
): Boolean = try {
    client.prepareGet(url).execute { response ->
        if (!response.status.isSuccess()) return@execute false
        val total = response.contentLength() ?: expectedSize.takeIf { it > 0 } ?: -1L
        val channel = response.bodyAsChannel()
        var read = 0L
        destFile.outputStream().use { out ->
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DOWNLOAD_BUFFER)
                while (!packet.exhausted()) {
                    val bytes = packet.readByteArray()
                    out.write(bytes)
                    read += bytes.size
                    if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        if (ApkIntegrity.verify(destFile, expectedSha256, expectedSize)) {
            onProgress(1f)
            true
        } else {
            destFile.delete()
            false
        }
    }
} catch (e: CancellationException) {
    destFile.delete()
    throw e // 用户取消/resetState：必须向上传播，不能当作"下载失败"吞掉
} catch (_: Throwable) {
    destFile.delete()
    false
}
