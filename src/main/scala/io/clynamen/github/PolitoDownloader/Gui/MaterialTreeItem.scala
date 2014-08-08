package io.clynamen.github.PolitoDownloader.Gui

import scalafx.scene.control.{CheckBox, TreeItem}

abstract class MaterialTreeItem(label : String, itemId : Int) extends TreeItem[String] {
  value = label

  // TODO: Move default implementation here (or at least delegate)
  def check(checked : Boolean)
  def checkable(checkable : Boolean)
  def checkbox : CheckBox
}
