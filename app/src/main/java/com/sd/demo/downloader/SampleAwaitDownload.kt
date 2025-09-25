package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sd.demo.downloader.databinding.SampleAwaitDownloadBinding
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.Downloader
import com.sd.lib.downloader.addTaskAwait
import com.sd.lib.downloader.downloadInfoFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SampleAwaitDownload : ComponentActivity() {
  private val _binding by lazy { SampleAwaitDownloadBinding.inflate(layoutInflater) }
  private val _downloadUrl = "https://dldir1v6.qq.com/weixin/Universal/Mac/WeChatMac.dmg"

  private val _downloader = Downloader.dirname("dmg")
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
      Downloader.downloadInfoFlow().collect { info ->
        logMsg { "collect $info" }
      }
    }
  }

  /** 开始下载 */
  private fun startDownload() {
    _awaitJob?.cancel()
    _awaitJob = lifecycleScope.launch {
      try {
        _downloader.addTaskAwait(
          DownloadRequest.Builder()
            .setPreferBreakpoint(true)
            .build(_downloadUrl)
        ).onSuccess {
          logMsg { "await onSuccess $it" }
        }.onFailure {
          logMsg { "await onFailure $it" }
        }
      } catch (e: Throwable) {
        logMsg { "await error $e" }
        throw e
      }
    }
  }

  /** 取消下载 */
  private fun cancelDownload() {
    _downloader.cancelTask(_downloadUrl)
  }

  /** 取消[_awaitJob]，不会取消下载任务 */
  private fun cancelJob() {
    _awaitJob?.cancel()
    _awaitJob = null
  }

  override fun onDestroy() {
    super.onDestroy()
    cancelDownload()
    _downloader.downloadDir {
      deleteAllTempFile()
      deleteAllDownloadFile()
    }
  }
}