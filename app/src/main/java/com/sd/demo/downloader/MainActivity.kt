package com.sd.demo.downloader

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.FDownloader
import com.sd.lib.downloader.IDownloadInfo
import com.sd.lib.downloader.IDownloader
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val URL = "https://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"

class MainActivity : ComponentActivity() {
    private val _scope = MainScope()
    private var _awaitJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btn_download).setOnClickListener {
            logMsg { "click download" }
            download()
        }

        findViewById<View>(R.id.btn_await_download).setOnClickListener {
            logMsg { "click await download" }
            awaitDownload()
        }

        findViewById<View>(R.id.btn_cancel).setOnClickListener {
            logMsg { "click cancel" }
            cancelDownload()
        }
    }

    private fun getDownloadRequest(): DownloadRequest {
        return DownloadRequest.Builder()
            // true-优先断点下载；false-不使用断点下载；null-跟随初始化配置
            .setPreferBreakpoint(true)
            // 下载地址
            .build(URL)
    }

    private fun download() {
        _awaitJob?.cancel()

        // 添加下载回调
        FDownloader.addCallback(_downloadCallback)

        // 添加下载任务
        FDownloader.addTask(getDownloadRequest())
    }

    private fun awaitDownload() {
        // 移除下载回调
        FDownloader.removeCallback(_downloadCallback)

        _awaitJob?.cancel()
        _scope.launch {
            FDownloader.awaitTask(
                request = getDownloadRequest(),
                onInitialized = {
                    logMsg { "await onInitialized" }
                },
                onProgress = {
                    logMsg { "await onProgress ${it.progress}" }
                },
            ).let { result ->
                result.onSuccess {
                    logMsg { "await success $it" }
                }
                result.onFailure {
                    logMsg { "await failure $it ${it.javaClass.name}" }
                }
            }
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
        override fun onInitialized(info: IDownloadInfo.Initialized) {
            logMsg { "onInitialized" }
        }

        override fun onProgress(info: IDownloadInfo.Progress) {
            logMsg { "onProgress ${info.progress}" }
        }

        override fun onSuccess(info: IDownloadInfo.Success) {
            logMsg { "onSuccess file:${info.file.absolutePath}" }
        }

        override fun onError(info: IDownloadInfo.Error) {
            logMsg { "onError ${info.exception.javaClass.name}" }
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

inline fun logMsg(block: () -> String) {
    Log.i("downloader-demo", block())
}