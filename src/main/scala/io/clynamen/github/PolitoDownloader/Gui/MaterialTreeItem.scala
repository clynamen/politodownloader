package io.clynamen.github.PolitoDownloader.Gui

import scalafx.scene.control.TreeItem

abstract class MaterialTreeItem(label : String, itemId : Int) extends TreeItem[String] {
  value = label

  // TODO: Move default implementation/checkbox here (or at least delegate)
  def check(checked : Boolean)
  def checkable(checkable : Boolean)
  def checked : Boolean
}
