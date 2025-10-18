package com.sd.lib.downloader

import java.io.File

interface DownloadDirScope {
  /** 删除子目录下的所有临时文件（不含下载中的临时文件），并返回删除的个数 */
  fun deleteAllTempFile(): Int

  /** 删除子目录下的所有下载文件（不含临时文件），并返回删除的个数 */
  fun deleteAllDownloadFile(): Int

  /** 不删除[url]对应的下载文件，其他逻辑同[deleteAllDownloadFile] */
  fun deleteAllDownloadFileExcept(url: String): Int

  /** 根据文件名称[name]获取下载文件，如果文件不存在则返回null */
  fun getDownloadFileByName(name: String): File?

  /**
   * 把[file]移动到下载目录并返回移动后的文件，如果文件已存在则覆盖，
   * 移动后的文件名为[name]，如果[name]为空则使用[file]的文件名。
   *
   * 注意：如果[file]或者[name]的扩展名为temp，则返回null
   */
  fun takeFile(file: File, name: String): File?
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

  override fun getDownloadFileByName(name: String): File? {
    return dir.existOrNullFileForName(dirname = dirname, name = name)
  }

  override fun takeFile(file: File, name: String): File? {
    return dir.takeFile(dirname = dirname, file = file, name = name)
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