package io.clynamen.github.PolitoDownloader.Gui

import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.scene.{Node, control => jfxsc}
import io.clynamen.github.PolitoDownloader.Client.{ContentId, DirectoryInfo}
import scala.language.implicitConversions


class DirectoryTreeItem(val directoryInfo : DirectoryInfo) extends ContentTreeItem {
  private var _contentFetched = false

  def contentFetched_=(value: Boolean) : Unit = _contentFetched = value
  def contentFetched = _contentFetched

  override def toString = directoryInfo.label
  override def visit(visitor: ContentTreeItemVisitor): Unit = visitor.visit(this)
  override def id: ContentId = directoryInfo.id


}

