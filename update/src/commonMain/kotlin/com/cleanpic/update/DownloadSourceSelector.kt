package com.cleanpic.update

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * 并发探测候选下载源，返回**最先成功(2xx)响应**的源；全部失败返回 null。
 *
 * 不做"判断国内/境外"的地域猜测（梯子分流、IP 库不准都会误判），而是用**实际连通性**说话：
 * 谁的响应头先到达，就用谁下整包。天然适配国内直连 / 境外梯子等任意网络。
 *
 * 探测**只读响应头**（[prepareGet] + [io.ktor.client.statement.HttpStatement.execute] 块内不读 body），
 * 收到 header 即判定并关闭连接，不下载整包——这点很关键：实测 Gitee 等 CDN **不支持 Range 请求**
 * （忽略 Range 直接返回 200 + 完整文件），所以探测不能靠 Range 限长，必须靠"只读 header 即断开"。
 * 与检查更新走同一 HttpClient/网络栈，保证"能检查就能下"。
 */
internal suspend fun selectFastestSource(
    client: HttpClient,
    candidates: List<String>,
): String? = coroutineScope {
    if (candidates.isEmpty()) return@coroutineScope null

    val winner = CompletableDeferred<String?>()
    val probes = candidates.map { url ->
        launch {
            try {
                val status = client.prepareGet(url).execute { it.status } // 只读 header，不消费 body
                if (status.isSuccess()) winner.complete(url) // complete 幂等：第一个胜出者生效
            } catch (e: CancellationException) {
                throw e // 取消（已有胜出者/外部取消）需正常传播，不可吞
            } catch (_: Throwable) {
                // 单源失败/超时不影响其它源继续竞速
            }
        }
    }
    // 所有探测都结束仍无人胜出 → null
    launch {
        probes.joinAll()
        winner.complete(null)
    }

    val result = winner.await()
    coroutineContext.cancelChildren() // 已有胜出者，取消尚未完成的慢探测，不再空等
    result
}
