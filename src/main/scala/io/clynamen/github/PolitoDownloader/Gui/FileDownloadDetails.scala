package io.clynamen.github.PolitoDownloader.Gui

class FileDownloadDetails(val url : String, val path: String) {
  override def equals(o: Any) = o match {
    case that: FileDownloadDetails => that.url.equals(url)
    case _ => false
  }
  override def hashCode = url.hashCode
}
