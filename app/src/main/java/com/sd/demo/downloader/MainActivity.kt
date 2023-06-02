package com.sd.demo.downloader

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.demo.downloader.ui.theme.AppTheme
import com.sd.lib.downloader.DownloadProgress
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.FDownloader
import com.sd.lib.downloader.IDownloader
import com.sd.lib.downloader.exception.DownloadException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

private const val URL = "https://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"

class MainActivity : ComponentActivity() {
    private val _scope = MainScope()
    private var _awaitJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content(
                    onClickDownload = {
                        download()
                    },
                    onClickAwaitDownload = {
                        awaitDownload()
                    },
                    onClickDelete = {
                        FDownloader.deleteDownloadFile(null)
                    },
                    onClickCancel = {
                        cancelDownload()
                    },
                )
            }
        }

        // 添加下载回调
        FDownloader.addCallback(_downloadCallback)
    }

    private fun getDownloadRequest(): DownloadRequest {
        return DownloadRequest.Builder()
            // true-优先断点下载；false-不使用断点下载；null-跟随初始化配置
            .setPreferBreakpoint(true)
            // 下载地址
            .build(URL)
    }

    private fun download() {
        val addTask = FDownloader.addTask(getDownloadRequest())
        logMsg { "download addTask:${addTask}" }
    }

    private fun awaitDownload() {
        _awaitJob?.cancel()
        _scope.launch {
            val result = FDownloader.awaitTask(getDownloadRequest())
            logMsg { "awaitDownload $result" }
        }.also { _awaitJob = it }
    }

    private fun cancelDownload() {
        // 取消下载任务
        FDownloader.cancelTask(URL)
    }

    /**
     * 下载回调
     */
    private val _downloadCallback: IDownloader.Callback = object : IDownloader.Callback {
        override fun onProgress(url: String, progress: DownloadProgress) {
            logMsg { "onProgress ${progress.progress}" }
        }

        override fun onSuccess(url: String, file: File) {
            logMsg { "onSuccess file:${file.absolutePath}" }
        }

        override fun onError(url: String, exception: DownloadException) {
            logMsg { "onError ${exception.javaClass.name}" }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _scope.cancel()

        // 移除下载回调
        FDownloader.removeCallback(_downloadCallback)
        // 删除所有临时文件（下载中的临时文件不会被删除）
        FDownloader.deleteTempFile()
        // 删除下载文件（临时文件不会被删除）
        FDownloader.deleteDownloadFile(null)
    }
}

@Composable
private fun Content(
    onClickDownload: () -> Unit,
    onClickAwaitDownload: () -> Unit,
    onClickDelete: () -> Unit,
    onClickCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = onClickDownload
        ) {
            Text(text = "download")
        }

        Button(
            onClick = onClickAwaitDownload
        ) {
            Text(text = "await download")
        }

        Button(
            onClick = onClickDelete
        ) {
            Text(text = "delete")
        }

        Button(
            onClick = onClickCancel
        ) {
            Text(text = "cancel")
        }
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("downloader-demo", block())
}