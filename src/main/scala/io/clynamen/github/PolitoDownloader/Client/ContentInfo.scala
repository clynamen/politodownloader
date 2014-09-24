package io.clynamen.github.PolitoDownloader.Client

import java.net.URI
import FileFormat._



abstract class ContentInfo(label: String, id: ContentId, pid: Option[ContentId])
case class DirectoryInfo(url: URI, label: String, id: ContentId,
                         pid: Option[ContentId]) extends ContentInfo(label, id, pid)
case class FileInfo(url: URI, label : String, id: ContentId, pid : Option[ContentId],
                    formats : Formats) extends ContentInfo(label, id, pid)
case class VideoDirectoryInfo(val linkData: VideoCourseLink, label: String, id: ContentId, pid: Option[ContentId]) extends ContentInfo(label, id, pid)

class Formats private (val formatList : Iterable[FileFormat]) {
}

object Formats {
  def apply( list : Iterable[FileFormat]) : Formats = new Formats(list)
  def apply( list : FileFormat*) : Formats = Formats(list)
}


