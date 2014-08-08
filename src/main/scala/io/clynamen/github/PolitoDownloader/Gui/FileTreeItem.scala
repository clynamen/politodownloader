package io.clynamen.github.PolitoDownloader.Gui

import java.lang
import javafx.beans.value.{ChangeListener, ObservableValue}

import scalafx.scene.control.CheckBox

class FileTreeItem(label : String, itemId: Int, url : String,
                   fileStatusChangeCallback: FileTreeItem.OnFileStatusChangeCallback)
    extends  MaterialTreeItem(label, itemId) {
  import io.clynamen.github.PolitoDownloader.Gui.FileTreeItem._

  val itemCheckbox = new CheckBox();
  itemCheckbox.selectedProperty().addListener(makeAddFileCheckboxListener(url, fileStatusChangeCallback))
  graphic = itemCheckbox

  override def check(checked : Boolean) = itemCheckbox.selected = checked
  override def checkable(checkable : Boolean) = {
    println(f"\n\n checkable $checkable \n\n")
    itemCheckbox.selected = false
    itemCheckbox.disable = !checkable
  }
  override  def checkbox = itemCheckbox
}

object FileTreeItem {
  type OnFileStatusChangeCallback = (FileDownloadDetails, Boolean) => Unit
  def makeAddFileCheckboxListener(url : String,
                                  statusChangeCallback: OnFileStatusChangeCallback) =
      new ChangeListener[java.lang.Boolean] {
        override def changed(observable: ObservableValue[_ <: lang.Boolean],
                         oldValue: lang.Boolean, newValue: lang.Boolean) = statusChangeCallback(
          new FileDownloadDetails(url, ""), newValue)
      }
}
