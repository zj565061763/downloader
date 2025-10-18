package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sd.demo.downloader.databinding.SampleTakeFileBinding
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.Downloader
import com.sd.lib.downloader.addTaskAwait
import com.sd.lib.downloader.downloadInfoFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SampleTakeFile : ComponentActivity() {
  private val _binding by lazy { SampleTakeFileBinding.inflate(layoutInflater) }
  private val _downloadUrl = "https://dldir1v6.qq.com/weixin/Universal/Windows/WeChatWin.exe"

  private val _downloader = Downloader.dirname("exe")
  private var _awaitJob: Job? = null
  private val _takeFilename = "1.exe"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(_binding.root)
    _binding.btnStartDownload.setOnClickListener {
      startDownload()
    }
    _binding.btnCancelDownload.setOnClickListener {
      cancelDownload()
    }

    lifecycleScope.launch {
      Downloader.downloadInfoFlow().collect { info ->
        logMsg { info.toString() }
      }
    }

    val takeFile = _downloader.downloadDir { getDownloadFileByName(_takeFilename) }
    logMsg { "takeFile:$takeFile" }
  }

  /** 开始下载 */
  private fun startDownload() {
    _awaitJob?.cancel()
    _awaitJob = lifecycleScope.launch {
      _downloader.addTaskAwait(
        DownloadRequest.Builder()
          .setPreferBreakpoint(true)
          .build(_downloadUrl)
      ).onSuccess { file ->
        logMsg { "await onSuccess $file" }
        val takeFile = _downloader.downloadDir { takeFile(file, _takeFilename) }
        logMsg { "await onSuccess takeFile:$takeFile" }
      }.onFailure {
        logMsg { "await onFailure $it" }
      }
    }
  }

  /** 取消下载 */
  private fun cancelDownload() {
    _downloader.cancelTask(_downloadUrl)
  }

  override fun onDestroy() {
    super.onDestroy()
    cancelDownload()
  }
}