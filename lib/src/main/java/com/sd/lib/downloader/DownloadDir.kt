package com.sd.lib.downloader

import java.io.File
import java.security.MessageDigest

internal interface DownloadDir {
  /**
   * [key]对应的临时文件
   */
  fun tempFileForKey(key: String): File?

  /**
   * [key]对应的文件，如果key有扩展名，则返回文件使用[key]的扩展名
   */
  fun fileForKey(key: String): File?

  /**
   * 访问所有临时文件
   */
  fun <T> tempFiles(block: (List<File>) -> T): T

  /**
   * 访问所有非临时文件
   */
  fun <T> files(block: (List<File>) -> T): T

  companion object {
    fun get(dir: File): DownloadDir {
      return DownloadDirImpl(dir = dir)
    }
  }
}

/** 临时文件扩展名 */
private const val TEMP_EXT = "temp"

private class DownloadDirImpl(dir: File) : DownloadDir {
  private val _dir = dir

  override fun tempFileForKey(key: String): File? {
    return newFileForKey(
      key = key,
      ext = TEMP_EXT,
    )
  }

  override fun fileForKey(key: String): File? {
    return newFileForKey(
      key = key,
      ext = key.substringAfterLast(".", ""),
    )
  }

  override fun <T> tempFiles(block: (List<File>) -> T): T {
    return listFiles { files ->
      block(files.filter { it.extension == TEMP_EXT })
    }
  }

  override fun <T> files(block: (List<File>) -> T): T {
    return listFiles { files ->
      block(files.filterNot { it.extension == TEMP_EXT })
    }
  }

  private fun <T> listFiles(block: (files: Array<File>) -> T): T {
    return modify {
      val files = it?.listFiles() ?: emptyArray()
      block(files)
    }
  }

  private fun newFileForKey(key: String, ext: String): File? {
    val dotExt = ext.takeIf { it.isEmpty() || it.startsWith(".") } ?: ".$ext"
    return modify { dir ->
      dir?.resolve(fMd5(key) + dotExt)
    }
  }

  private fun <T> modify(block: (dir: File?) -> T): T {
    synchronized(this@DownloadDirImpl) {
      val directory = if (_dir.fMakeDirs()) _dir else null
      return block(directory)
    }
  }
}

private fun File.fMakeDirs(): Boolean {
  if (this.isDirectory) return true
  if (this.isFile) this.delete()
  return this.mkdirs()
}

private fun fMd5(input: String): String {
  val md5Bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
  return buildString {
    for (byte in md5Bytes) {
      val hex = Integer.toHexString(0xff and byte.toInt())
      if (hex.length == 1) append("0")
      append(hex)
    }
  }
}