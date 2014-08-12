package io.clynamen.github.PolitoDownloader.Gui

class FileDownloadDetails(val fileId : Int, val url : String, val path: String) {
  override def equals(o: Any) = o match {
    case that: FileDownloadDetails => that.fileId.equals(fileId)
    case _ => false
  }
  override def hashCode = fileId.hashCode
}
