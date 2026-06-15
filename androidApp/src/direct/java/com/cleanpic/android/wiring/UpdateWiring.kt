package com.cleanpic.android.wiring

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.android.BuildConfig
import com.cleanpic.di.ServiceLocator
import com.cleanpic.log.logger
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.AppHooks
import com.cleanpic.update.AndroidUpdateInstaller
import com.cleanpic.update.DownloadProgressDialog
import com.cleanpic.update.DownloadState
import com.cleanpic.update.InstallingDialog
import com.cleanpic.update.UpdateChecker
import com.cleanpic.update.UpdateCheckResult
import com.cleanpic.update.UpdateDialog
import com.cleanpic.update.UpdateFailedDialog
import com.cleanpic.update.UpdateResultCache
import com.cleanpic.update.UpdateStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val log = logger("Update")

object UpdateWiring {

    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var checker: UpdateChecker? = null
    private var installer: AndroidUpdateInstaller? = null

    fun provideHooks(context: Context): AppHooks {
        if (!BuildConfig.UPDATE_ENABLED) return AppHooks.Empty
        // 更新检测只走 Gitee raw version.json（发布不再部署 Cloudflare Worker，
        // 保留 Worker 作回退会在 Gitee 失败时回退到过期版本信息，故移除）。
        val endpoints = buildList {
            if (BuildConfig.UPDATE_API_URL_CN.isNotBlank()) add(BuildConfig.UPDATE_API_URL_CN)
        }
        checker = UpdateChecker(endpoints)
        installer = AndroidUpdateInstaller(context.applicationContext)
        return DirectAppHooks(checker!!, installer!!)
    }

    private class DirectAppHooks(
        private val checker: UpdateChecker,
        private val installer: AndroidUpdateInstaller
    ) : AppHooks {

        override fun onAppStart() {
            if (!ServiceLocator.appSettings.autoCheckUpdate) return
            updateScope.launch {
                log.i { "更新检测开始" }
                runCatching {
                    val result = checker.checkForUpdate()
                    UpdateResultCache.update(result)
                    if (result.status == UpdateStatus.UP_TO_DATE) {
                        log.i { "更新检测：已是最新" }
                    } else {
                        log.i { "更新检测：发现新版本 ${result.updateInfo?.version}" }
                    }
                }.onFailure {
                    log.w { "更新检测失败：${it::class.simpleName}" }
                }
            }
        }

        @Composable
        override fun HomeOverlay() {
            HomeUpdateOverlay(checker, installer)
        }

        @Composable
        override fun SettingsExtras() {
            SettingsUpdateSection(checker, installer)
        }
    }
}

@Composable
private fun HomeUpdateOverlay(
    checker: UpdateChecker,
    installer: AndroidUpdateInstaller
) {
    val theme by ServiceLocator.themeManager.currentTheme.collectAsState()
    val updateResult by UpdateResultCache.value.collectAsState()
    var dialogShown by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    if (!dialogShown && updateResult.status != UpdateStatus.UP_TO_DATE && updateResult.updateInfo != null) {
        showDialog = true
        dialogShown = true
    }

    if (showDialog) {
        val info = updateResult.updateInfo
        if (info != null) {
            UpdateDialog(
                theme = theme,
                updateInfo = info,
                isForceUpdate = updateResult.status == UpdateStatus.FORCE_UPDATE,
                onUpdate = {
                    showDialog = false
                    log.i { "更新下载开始" }
                    installer.startUpdate(info)
                },
                onDismiss = { showDialog = false }
            )
        }
    }

    val downloadState by installer.downloadState.collectAsState()
    val downloadProgress by installer.downloadProgress.collectAsState()

    // 仅在下载状态发生变化时记录终态一次，避免重组期重复打点
    LaunchedEffect(downloadState) {
        when (downloadState) {
            DownloadState.DOWNLOADED -> log.i { "更新下载完成" }
            DownloadState.FAILED -> log.e { "更新下载失败" }
            else -> {}
        }
    }

    when (downloadState) {
        DownloadState.DOWNLOADING -> DownloadProgressDialog(theme = theme, progress = downloadProgress)
        DownloadState.INSTALLING -> InstallingDialog(theme = theme)
        DownloadState.FAILED -> {
            val info = updateResult.updateInfo
            UpdateFailedDialog(
                theme = theme,
                onRetry = {
                    installer.resetState()
                    if (info != null) {
                        log.i { "更新下载开始" }
                        installer.startUpdate(info)
                    }
                },
                onDismiss = { installer.resetState() }
            )
        }
        else -> {}
    }
}

@Composable
private fun SettingsUpdateSection(
    checker: UpdateChecker,
    installer: AndroidUpdateInstaller
) {
    val theme by ServiceLocator.themeManager.currentTheme.collectAsState()
    var autoCheckUpdate by remember { mutableStateOf(ServiceLocator.appSettings.autoCheckUpdate) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var checkResultMessage by remember { mutableStateOf<String?>(null) }
    var manualCheckResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    val scope = rememberCoroutineScope()

    // 用 collectAsState 而非 .value 直读，确保启动期 onAppStart 异步写入缓存后设置页能立即重组
    val cachedResult by UpdateResultCache.value.collectAsState()
    val updateResult: UpdateCheckResult? = manualCheckResult
        ?: cachedResult.takeIf { it.status != UpdateStatus.UP_TO_DATE }

    UpdateSectionUi(
        theme = theme,
        radius = theme.borderRadius,
        autoCheckUpdate = autoCheckUpdate,
        onAutoCheckUpdateChange = { enabled ->
            autoCheckUpdate = enabled
            ServiceLocator.appSettings.autoCheckUpdate = enabled
        },
        isCheckingUpdate = isCheckingUpdate,
        onCheckUpdate = {
            scope.launch {
                isCheckingUpdate = true
                checkResultMessage = null
                log.i { "更新检测开始" }
                runCatching {
                    val result = checker.checkForUpdate()
                    UpdateResultCache.update(result)
                    manualCheckResult = result
                    if (result.status == UpdateStatus.UP_TO_DATE) {
                        log.i { "更新检测：已是最新" }
                    } else {
                        log.i { "更新检测：发现新版本 ${result.updateInfo?.version}" }
                    }
                    checkResultMessage = when (result.status) {
                        UpdateStatus.UP_TO_DATE -> "已是最新版本"
                        UpdateStatus.OPTIONAL_UPDATE -> "发现新版本 v${result.updateInfo?.version}"
                        UpdateStatus.FORCE_UPDATE -> "发现新版本 v${result.updateInfo?.version}（需要更新）"
                    }
                }.onFailure {
                    log.w { "更新检测失败：${it::class.simpleName}" }
                    checkResultMessage = "网络不可用，请稍后再试"
                }
                isCheckingUpdate = false
            }
        },
        updateResult = updateResult,
        checkResultMessage = checkResultMessage,
        onStartUpdate = {
            val info = updateResult?.updateInfo ?: return@UpdateSectionUi
            log.i { "更新下载开始" }
            installer.startUpdate(info)
        },
        isDebugBuild = ServiceLocator.isDebugBuild,
        onSimulateDownload = { installer.simulateDownload() }
    )
}

@Composable
private fun UpdateSectionUi(
    theme: ThemeTokens,
    radius: Float,
    autoCheckUpdate: Boolean,
    onAutoCheckUpdateChange: (Boolean) -> Unit,
    isCheckingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    updateResult: UpdateCheckResult?,
    checkResultMessage: String?,
    onStartUpdate: () -> Unit,
    isDebugBuild: Boolean,
    onSimulateDownload: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 自动检查更新开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius.dp))
                .background(Color(theme.colorSurface))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "自动检查更新",
                fontSize = 14.sp,
                color = Color(theme.colorText)
            )
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (autoCheckUpdate) Color(theme.colorPrimary)
                        else Color(theme.colorTextSecondary).copy(alpha = 0.3f)
                    )
                    .clickable { onAutoCheckUpdateChange(!autoCheckUpdate) }
                    .testTag("auto_check_update_toggle"),
                contentAlignment = if (autoCheckUpdate) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 检查更新按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius.dp))
                .background(Color(theme.colorSurface))
                .clickable(enabled = !isCheckingUpdate) { onCheckUpdate() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("check_update_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isCheckingUpdate) "检查中..." else "检查更新",
                fontSize = 14.sp,
                color = Color(theme.colorText)
            )
            if (updateResult != null && updateResult.status != UpdateStatus.UP_TO_DATE) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
        }

        // 检查结果文字
        if (checkResultMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = checkResultMessage,
                fontSize = 12.sp,
                color = Color(theme.colorTextSecondary),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("check_result_message")
            )
        }

        // 有新版本时显示"立即更新"按钮
        if (updateResult != null && updateResult.status != UpdateStatus.UP_TO_DATE) {
            Spacer(modifier = Modifier.height(8.dp))
            val info = updateResult.updateInfo
            if (info != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radius.dp))
                        .background(Color(theme.colorPrimary))
                        .clickable { onStartUpdate() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("start_update_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "立即更新到 v${info.version}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // 调试按钮：模拟下载
        if (isDebugBuild) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.dp))
                    .background(Color(theme.colorSurface))
                    .clickable { onSimulateDownload() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("simulate_download_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "[调试] 模拟下载",
                    fontSize = 12.sp,
                    color = Color(theme.colorTextSecondary)
                )
            }
        }
    }
}
