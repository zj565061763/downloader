package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sd.demo.downloader.databinding.SampleAwaitDownloadBinding
import com.sd.lib.downloader.DownloadCallback
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.FDownloader
import com.sd.lib.downloader.IDownloadInfo
import com.sd.lib.downloader.awaitTask
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SampleAwaitDownload : ComponentActivity() {
    private val _binding by lazy { SampleAwaitDownloadBinding.inflate(layoutInflater) }
    private val url = "https://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"

    private val _scope = MainScope()
    private var _awaitJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        _binding.btnStartDownload.setOnClickListener { startDownload() }
        _binding.btnCancelDownload.setOnClickListener { cancelDownload() }
        _binding.btnCancelJob.setOnClickListener { cancelJob() }
    }

    /**
     * 开始下载
     */
    private fun startDownload() {
        val request = DownloadRequest.Builder()
            .setPreferBreakpoint(true)
            .build(url)

        _awaitJob?.cancel()
        _awaitJob = _scope.launch {
            FDownloader.awaitTask(
                request = request,
                callback = object : DownloadCallback() {
                    override fun onInitialized(info: IDownloadInfo.Initialized) {
                        logMsg { "callback onInitialized" }
                    }

                    override fun onProgress(info: IDownloadInfo.Progress) {
                        logMsg { "callback onProgress ${info.progress}" }
                    }

                    override fun onSuccess(info: IDownloadInfo.Success) {
                        logMsg { "callback onSuccess file:${info.file.absolutePath}" }
                    }

                    override fun onError(info: IDownloadInfo.Error) {
                        logMsg { "callback onError ${info.exception}" }
                    }
                },
            ).let { result ->
                result.onSuccess {
                    logMsg { "await onSuccess $it" }
                }.onFailure {
                    logMsg { "await onFailure $it" }
                }
            }
        }
    }

    /**
     * 取消下载
     */
    private fun cancelDownload() {
        // 取消下载任务
        FDownloader.cancelTask(url)
    }

    /**
     * 取消[_awaitJob]
     */
    private fun cancelJob() {
        _awaitJob?.cancel()
        _awaitJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _scope.cancel()

        cancelDownload()

        // 删除所有临时文件（下载中的临时文件不会被删除）
        FDownloader.deleteTempFile()

        // 删除下载文件（临时文件不会被删除）
        FDownloader.deleteDownloadFile(null)
    }
}