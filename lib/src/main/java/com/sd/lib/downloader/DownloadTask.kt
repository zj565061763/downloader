package com.sd.lib.downloader

import java.util.concurrent.atomic.AtomicReference

internal class DownloadTask(
  val url: String,
  progressNotifyStrategy: DownloadProgressNotifyStrategy,
) {
  private val _state: AtomicReference<DownloadState> = AtomicReference(DownloadState.None)
  private val _transmitParams = TransmitParams(progressNotifyStrategy)

  fun notifyInitialized(): Boolean {
    return _state.compareAndSet(DownloadState.None, DownloadState.Initialized)
  }

  fun notifyProgress(total: Long, current: Long): DownloadInfo.Progress? {
    return when (_state.get()) {
      DownloadState.None -> error("Task not initialized")
      DownloadState.Initialized, DownloadState.Progress -> {
        synchronized(_transmitParams) {
          if (_transmitParams.transmit(total, current)) {
            _transmitParams.toProgress(url)
          } else {
            null
          }
        }
      }
      else -> null
    }
  }

  fun notifySuccess(): Boolean {
    return when (val state = _state.get()) {
      DownloadState.None -> error("Task not initialized")
      DownloadState.Initialized,
      DownloadState.Progress,
        -> _state.compareAndSet(state, DownloadState.Success)
      else -> false
    }
  }

  fun notifyError(): Boolean {
    return when (val state = _state.get()) {
      DownloadState.Success,
      DownloadState.Error,
        -> false
      else -> _state.compareAndSet(state, DownloadState.Error)
    }
  }

  private enum class DownloadState {
    None,
    Initialized,
    Progress,
    Success,
    Error,
  }
}

private fun TransmitParams.toProgress(url: String): DownloadInfo.Progress {
  return DownloadInfo.Progress(
    url = url,
    total = this.total,
    current = this.current,
    progress = this.progress,
  )
}

private class TransmitParams(
  val progressNotifyStrategy: DownloadProgressNotifyStrategy,
) {
  /** 总数量 */
  var total: Long = 0
    private set

  /** 已传输数量 */
  var current: Long = 0
    private set

  /** 传输进度[0-100] */
  var progress: Float = 0f
    private set

  /**
   * 传输
   * @param total 总量
   * @param current 当前传输量
   * @return true-进度发生了变化
   */
  fun transmit(total: Long, current: Long): Boolean {
    val oldProgress = progress

    if (total <= 0 || current <= 0) {
      this.total = 0
      this.current = 0
      this.progress = 0f
      return this.progress != oldProgress
    }

    this.total = total
    this.current = current
    val newProgress = (current.toDouble() / total.toDouble() * 100).toFloat().coerceAtMost(100f)

    if (newProgress >= 100f) {
      if (newProgress > oldProgress) {
        this.progress = 100f
        return true
      } else {
        return false
      }
    }

    when (progressNotifyStrategy) {
      is DownloadProgressNotifyStrategy.WhenProgressIncreased -> {
        val increased = progressNotifyStrategy.increased
        if ((newProgress - oldProgress) >= increased) {
          this.progress = newProgress
        }
      }
    }

    return this.progress > oldProgress
  }

  override fun toString(): String {
    return "${current}/${total} $progress"
  }
}