package com.sd.lib.downloader

import java.io.File

interface DownloadDirScope {
  /** 删除子目录下的所有临时文件（不含下载中的临时文件），并返回删除的个数 */
  fun deleteAllTempFile(): Int

  /** 删除子目录下的所有下载文件（不含临时文件），并返回删除的个数 */
  fun deleteAllDownloadFile(): Int

  /** 不删除[url]对应的下载文件，其他逻辑同[deleteAllDownloadFile] */
  fun deleteAllDownloadFileExcept(url: String): Int
}

internal class DownloadDirScopeImpl(
  private val dirname: String,
  private val dir: DownloadDir,
) : DownloadDirScope {
  override fun deleteAllTempFile(): Int {
    return dir.tempFiles(dirname = dirname) { files ->
      var count = 0
      files.forEach { file ->
        if (file.delete()) count++
      }
      count
    }
  }

  override fun deleteAllDownloadFile(): Int {
    return deleteAllDownloadFile { true }
  }

  override fun deleteAllDownloadFileExcept(url: String): Int {
    val exceptFile = dir.fileForKey(dirname = dirname, key = url)
    return deleteAllDownloadFile { it != exceptFile }
  }

  private fun deleteAllDownloadFile(block: (File) -> Boolean): Int {
    return dir.files(dirname = dirname) { files ->
      var count = 0
      files.forEach { file ->
        if (block(file)) {
          if (file.delete()) count++
        }
      }
      count
    }
  }
}