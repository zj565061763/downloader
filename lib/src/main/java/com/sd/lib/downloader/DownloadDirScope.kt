package com.sd.lib.downloader

interface DownloadDirScope {
  /** 删除所有下载文件（不含临时文件），并返回删除的文件个数 */
  fun deleteAllDownloadFile(): Int

  /** 删除[url]对应的下载文件，并返回本次调用是否删除了文件 */
  fun deleteDownloadFile(url: String): Boolean
}

internal class DownloadDirScopeImpl(
  private val dir: DownloadDir,
) : DownloadDirScope {
  override fun deleteAllDownloadFile(): Int {
    return dir.files { files ->
      var count = 0
      files.forEach { file ->
        if (file.deleteRecursively()) count++
      }
      count
    }
  }

  override fun deleteDownloadFile(url: String): Boolean {
    return dir.deleteFileForKey(key = url)
  }
}