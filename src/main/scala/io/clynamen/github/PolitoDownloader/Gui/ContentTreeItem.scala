package io.clynamen.github.PolitoDownloader.Gui

import io.clynamen.github.PolitoDownloader.Client.ContentId

import scalafx.scene.control.{Control, TreeItem}

abstract class ContentTreeItem extends CheckboxTreeItemView {
  def visit(visitor: ContentTreeItemVisitor)
  def id : ContentId
  def graphic : Option[javafx.scene.Node] = None
}

class PlaceholderTreeItem extends  ContentTreeItem {
  override def visit(visitor: ContentTreeItemVisitor): Unit = {}

  override def id: ContentId = throw new NotImplementedError("Placeholder has no id!")
}

trait ContentTreeItemVisitor {
  def visit(item: DocumentTreeItem)
  def visit(item: DirectoryTreeItem)
  def visit(item: VideoDirectoryTreeItem)
}
