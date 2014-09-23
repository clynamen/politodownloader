package io.clynamen.github.PolitoDownloader.Client

object ContentType extends Enumeration {
  type ContentType = Value
  val File, Video, Directory, VideoSet, Course = Value
}

import ContentType._

class ContentId(val contentType: ContentType, val itemId: Integer) {

  override def toString: String = contentType.toString + itemId

  override def equals(o: Any) = o match {
    case that: ContentId => that.contentType == contentType && that.itemId == itemId
    case _ => false
  }

  override def hashCode = itemId.hashCode

}
