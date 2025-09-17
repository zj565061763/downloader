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
    // 删除所有临时文件（下载中的临时文件不会被删除）
    FDownloader.deleteTempFile()
    // 删除所有下载文件（临时文件不会被删除）
    FDownloader.deleteDownloadFile()
  }
}

inline fun logMsg(block: () -> String) {
  Log.i("downloader-demo", block())
}