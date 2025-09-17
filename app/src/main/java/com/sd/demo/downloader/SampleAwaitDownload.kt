package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sd.demo.downloader.databinding.SampleAwaitDownloadBinding
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.Downloader
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
      FDownloader.addTaskAwait(
        request = DownloadRequest.Builder()
          .setPreferBreakpoint(true)
          .build(url),
        callback = object : Downloader.Callback {
          override fun onDownloadInfo(info: IDownloadInfo) {
            when (info) {
              is IDownloadInfo.Initialized -> logMsg { "callback onInitialized" }
              is IDownloadInfo.Progress -> logMsg { "callback onProgress ${info.progress}" }
              is IDownloadInfo.Success -> logMsg { "callback onSuccess file:${info.file.absolutePath}" }
              is IDownloadInfo.Error -> logMsg { "callback onError ${info.exception}" }
            }
          }
        },
      ).onSuccess {
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