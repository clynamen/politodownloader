package io.clynamen.github.PolitoDownloader.Gui

import javafx.beans.value.{ChangeListener, ObservableValue}
import io.clynamen.github.PolitoDownloader.Client.{VideoFileFormat, VideoFileInfo, ContentId}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{ChoiceBox, Label}

class VideoTreeItem(val info: VideoFileInfo) extends  ContentTreeItem() {
  override def toString = info.label

  override def visit(visitor: ContentTreeItemVisitor): Unit = visitor.visit(this)

  override def id: ContentId = info.id

  val formatChoiceBox = new ChoiceBox[VideoFileFormat.Value]() {
    items = ObservableBuffer(VideoFileFormat.values.toList)
    value = VideoFileFormat.MP4
  }

  override def graphic: Option[javafx.scene.Node] = None

  var downloaded_ = false
  def downloaded_=(value: Boolean) = downloaded_ = value
  def downloaded = downloaded_
}
