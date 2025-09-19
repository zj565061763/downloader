package com.sd.demo.downloader

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.sd.demo.downloader.databinding.ActivityMainBinding
import com.sd.lib.downloader.FDownloader

class MainActivity : ComponentActivity() {
  private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(_binding.root)
    _binding.btnSampleDownload.setOnClickListener {
      startActivity(Intent(this, SampleDownload::class.java))
    }
    _binding.btnSampleAwaitDownload.setOnClickListener {
      startActivity(Intent(this, SampleAwaitDownload::class.java))
    }
  }

  override fun onResume() {
    super.onResume()
    // 删除所有下载文件（不含临时文件）
    FDownloader.deleteDownloadFile { it.deleteRecursively() }
  }
}

inline fun logMsg(block: () -> String) {
  Log.i("sd-demo", block())
}