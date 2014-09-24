package io.clynamen.github.PolitoDownloader.Client

object FileFormat extends Enumeration {
  type FileFormat = Value
  val JPEG, MP4, DOC = Value
}

object VideoFileFormat extends Enumeration {
  type VideoFileFormat = Value
  val MP4, iPhone, iPod, Mobile, Audio = Value
}
