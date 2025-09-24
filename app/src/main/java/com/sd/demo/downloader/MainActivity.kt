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
    FDownloader.downloadDir {
      // 删除所有下载文件（不含临时文件），并返回删除的文件个数
      deleteDownloadFiles().also { count ->
        logMsg { "deleteDownloadFiles count:$count" }
      }

      // 删除[url]对应的下载文件，并返回本次调用是否删除了文件
      deleteDownloadFile(url = "http://www.baidu.com/fake.zip").also { delete ->
        logMsg { "deleteDownloadFile delete:$delete" }
      }
    }
  }
}

inline fun logMsg(block: () -> String) {
  Log.i("sd-demo", block())
}