package io.clynamen.github.PolitoDownloader.Client

trait DownloadListener {
  def started(filename: String)
  def partDownloaded(size: Long)
  def completed()
  def error(cause: String)
}
