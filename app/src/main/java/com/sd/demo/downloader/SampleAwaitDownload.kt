package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sd.demo.downloader.databinding.SampleAwaitDownloadBinding
import com.sd.lib.downloader.DownloadInfo
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.Downloader
import com.sd.lib.downloader.FDownloader
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
    _binding.btnStartDownload.setOnClickListener {
      startDownload()
    }
    _binding.btnCancelDownload.setOnClickListener {
      cancelDownload()
    }
    _binding.btnCancelJob.setOnClickListener {
      cancelJob()
    }

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
    _awaitJob?.cancel()
    _awaitJob = lifecycleScope.launch {
      val request = DownloadRequest.Builder()
        .setPreferBreakpoint(true)
        .build(url)

      val callback = object : Downloader.Callback {
        override fun onDownloadInfo(info: DownloadInfo) {
          when (info) {
            is DownloadInfo.Initialized -> logMsg { "callback Initialized" }
            is DownloadInfo.Progress -> logMsg { "callback Progress ${info.progress}" }
            is DownloadInfo.Cancelling -> logMsg { "callback Cancelling" }
            is DownloadInfo.Success -> logMsg { "callback Success file:${info.file.absolutePath}" }
            is DownloadInfo.Error -> logMsg { "callback Error ${info.exception}" }
          }
        }
      }

      FDownloader.addTaskAwait(request = request, callback = callback)
        .onSuccess {
          logMsg { "await onSuccess $it" }
        }.onFailure {
          logMsg { "await onFailure $it" }
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