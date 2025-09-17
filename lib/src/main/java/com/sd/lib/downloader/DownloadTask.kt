package com.sd.lib.downloader

import java.util.concurrent.atomic.AtomicReference

internal class DownloadTask(
  val url: String,
) {
  private val _state: AtomicReference<DownloadState> = AtomicReference(DownloadState.None)
  private val _transmitParams = TransmitParams()

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
    speedBps = this.speedBps,
  )
}

private class TransmitParams {
  private var _lastSpeedTime: Long = 0
  private var _lastSpeedCount: Long = 0

  /** 总数量 */
  var total: Long = 0
    private set

  /** 已传输数量 */
  var current: Long = 0
    private set

  /** 传输进度[0-100] */
  var progress: Int = 0
    private set

  /** 传输速率（B/S） */
  var speedBps: Int = 0
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
      reset()
      return oldProgress != progress
    }

    this.total = total
    this.current = current

    val newProgress = (current * 100 / total).toInt().coerceAtMost(100)
    this.progress = newProgress

    if (_lastSpeedTime <= 0) {
      _lastSpeedTime = System.currentTimeMillis()
    }

    if (newProgress > oldProgress) {
      val time = System.currentTimeMillis()
      val changeTime = time - _lastSpeedTime
      if (changeTime > 0) {
        val changeCount = (current - _lastSpeedCount).coerceAtLeast(0)
        speedBps = (changeCount * (1000f / changeTime)).toInt()
      } else {
        speedBps = 0
      }
      _lastSpeedTime = time
      _lastSpeedCount = current
    }

    return newProgress > oldProgress
  }

  private fun reset() {
    _lastSpeedTime = 0
    _lastSpeedCount = 0
    total = 0
    current = 0
    progress = 0
    speedBps = 0
  }

  override fun toString(): String {
    return "${current}/${total} $progress $speedBps"
  }
}