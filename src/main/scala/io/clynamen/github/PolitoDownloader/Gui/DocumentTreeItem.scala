package io.clynamen.github.PolitoDownloader.Gui

import javafx.beans.value.{ChangeListener, ObservableValue}
import io.clynamen.github.PolitoDownloader.Client.{ContentId, FileInfo}
import scalafx.scene.control.Label

class DocumentTreeItem(val info: FileInfo) extends  ContentTreeItem() {
  override def toString = info.label

  override def visit(visitor: ContentTreeItemVisitor): Unit = visitor.visit(this)

  override def id: ContentId = info.id

  override def graphic: Option[javafx.scene.Node] = Some(Label("asdf"))
}

