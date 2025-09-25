package com.sd.lib.downloader

import java.io.File
import java.security.MessageDigest

internal interface DownloadDir {
  /** [key]对应的临时文件 */
  fun tempFileForKey(dirname: String, key: String): File?

  /** [key]对应的文件，如果key有扩展名，则使用[key]的扩展名 */
  fun fileForKey(dirname: String, key: String): File?

  fun existOrNullFileForKey(dirname: String, key: String): File?

  /** 访问所有非临时文件 */
  fun <T> files(dirname: String, block: (List<File>) -> T): T

  companion object {
    fun get(dir: File): DownloadDir {
      return DownloadDirImpl(dir = dir)
    }
  }
}

/** 临时文件扩展名 */
private const val ExtTemp = "temp"

private class DownloadDirImpl(dir: File) : DownloadDir {
  private val _dir = dir

  override fun tempFileForKey(dirname: String, key: String): File? {
    return extFileForKey(
      dirname = dirname,
      key = key,
      ext = ExtTemp,
    ) { it }
  }

  override fun fileForKey(dirname: String, key: String): File? {
    return fileForKey(
      dirname = dirname,
      key = key,
    ) { it }
  }

  override fun existOrNullFileForKey(dirname: String, key: String): File? {
    return fileForKey(
      dirname = dirname,
      key = key,
      checkExist = true,
    ) { it }
  }

  override fun <T> files(dirname: String, block: (List<File>) -> T): T {
    return listFiles(dirname = dirname) { files ->
      block(files.filter { it.extension != ExtTemp })
    }
  }

  private fun <T> listFiles(dirname: String, block: (files: Array<File>) -> T): T {
    return modify(dirname = dirname) { dir ->
      block(dir?.listFiles() ?: emptyArray())
    }
  }

  private inline fun <T> fileForKey(
    dirname: String,
    key: String,
    checkExist: Boolean = false,
    block: (File?) -> T,
  ): T {
    return extFileForKey(
      dirname = dirname,
      key = key,
      ext = key.substringAfterLast(".", ""),
      checkExist = checkExist,
      block = block,
    )
  }

  private inline fun <T> extFileForKey(
    dirname: String,
    key: String,
    ext: String,
    checkExist: Boolean = false,
    block: (File?) -> T,
  ): T {
    val dotExt = ext.takeIf { it.isEmpty() || it.startsWith(".") } ?: ".$ext"
    return modify(dirname = dirname) { dir ->
      val keyFile = dir?.resolve(fMd5(key) + dotExt)
      block(if (checkExist) keyFile?.takeIf { it.exists() } else keyFile)
    }
  }

  private inline fun <T> modify(dirname: String, block: (dir: File?) -> T): T {
    synchronized(this@DownloadDirImpl) {
      return block(_dir.resolve(dirname).takeIf { it.fMakeDirs() })
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