package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sd.demo.downloader.databinding.SampleAwaitDownloadBinding
import com.sd.lib.downloader.DownloadCallback
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.FDownloader
import com.sd.lib.downloader.IDownloadInfo
import com.sd.lib.downloader.addTaskAwait
import com.sd.lib.downloader.downloadInfoFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SampleAwaitDownload : ComponentActivity() {
  private val _binding by lazy { SampleAwaitDownloadBinding.inflate(layoutInflater) }
  private val url = "https://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"

  private var _awaitJob: Job? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(_binding.root)
    _binding.btnStartDownload.setOnClickListener { startDownload() }
    _binding.btnCancelDownload.setOnClickListener { cancelDownload() }
    _binding.btnCancelJob.setOnClickListener { cancelJob() }

    collectDownloadInfo()
  }

  /**
   * 收集下载信息
   */
  private fun collectDownloadInfo() {
    lifecycleScope.launch {
      FDownloader.downloadInfoFlow().collect { info ->
        logMsg { "collect $info" }
      }
    }
  }

  /**
   * 开始下载
   */
  private fun startDownload() {
    val request = DownloadRequest.Builder()
      .setPreferBreakpoint(true)
      .build(url)

    _awaitJob?.cancel()
    _awaitJob = lifecycleScope.launch {
      FDownloader.addTaskAwait(
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
   * 取消[_awaitJob]，不会取消下载任务
   */
  private fun cancelJob() {
    _awaitJob?.cancel()
    _awaitJob = null
  }

  override fun onDestroy() {
    super.onDestroy()
    cancelDownload()
  }
}