package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sd.demo.downloader.databinding.SampleAwaitDownloadBinding
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.FDownloader
import com.sd.lib.downloader.IDownloadInfo
import com.sd.lib.downloader.IDownloader
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
                callback = object : IDownloader.Callback {
                    override fun onDownloadInfo(info: IDownloadInfo) {
                        when (info) {
                            is IDownloadInfo.Initialized -> logMsg { "callback Initialized" }
                            is IDownloadInfo.Progress -> logMsg { "callback Progress ${info.progress}" }
                            is IDownloadInfo.Success -> logMsg { "callback Success file:${info.file.absolutePath}" }
                            is IDownloadInfo.Error -> logMsg { "callback Error ${info.exception.javaClass.name}" }
                        }
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