package com.sd.demo.downloader

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.sd.demo.downloader.databinding.ActivityMainBinding

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
    _binding.btnSampleTakeFile.setOnClickListener {
      startActivity(Intent(this, SampleTakeFile::class.java))
    }
  }
}

inline fun logMsg(block: () -> String) {
  Log.i("sd-demo", block())
}