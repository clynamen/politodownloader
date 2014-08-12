package io.clynamen.github.PolitoDownloader.Gui

import java.lang
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.event.EventHandler
import javafx.scene.control.TreeItem.TreeModificationEvent
import javafx.scene.{Node, control => jfxsc}

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.scene.control._
import scalafx.scene.control.CheckBox._


class DirTreeItem(id: Int, label: String, url: String,
                  fetchDir : DirTreeItem.DirFetcher) extends MaterialTreeItem(label, id) {
    import io.clynamen.github.PolitoDownloader.Gui.DirTreeItem._
    private def newPlaceholder() = new TreeItem[String]()
    var fetched = false
    val placeHolder = newPlaceholder()
    value = label
    children.add(placeHolder)

    val itemCheckbox = new CheckBox();
    itemCheckbox.selectedProperty().addListener(makeMatDirCheckboxListener(this))
    addEventHandler(jfxsc.TreeItem.branchExpandedEvent[String], new EventHandler[TreeModificationEvent[String]] {
      // TODO:  use 'handle' here
      override def handle(event: TreeModificationEvent[String]): Unit = {
        if (!fetched) expandItem(false)
        removeEventHandler(jfxsc.TreeItem.branchExpandedEvent[String], this)
      }
    })
    graphic = itemCheckbox

    def expandItem(recursive: Boolean) {
      if(fetched) return
      fetched = true
      fetchDir(id, url, recursive)
      children.remove(placeHolder)
      expanded = true
    }

    override def check(checked : Boolean) = itemCheckbox.selected = checked

    override def checkable(checkable : Boolean) =  itemCheckbox.disable = !checkable

    override def checked : Boolean = itemCheckbox.selected.value

    def checkAllRecursive(checked: Boolean) = expandAndCheckRecursive(this, checked)
}

object  DirTreeItem {
  type DirFetcher = (Int, String, Boolean) => Unit

    def expandAndCheckRecursive(treeItem: DirTreeItem, checked: Boolean): Unit = {
      treeItem.expandItem(true)

      def trySelectCheckbox(n: Node) {
        n match {
          case c: scalafx.scene.control.CheckBox =>
            c.selected = checked
          case c: javafx.scene.control.CheckBox =>
            c.selected = checked
          case _ => ;
        }
      }

      treeItem.children.foreach(t => jfxTreeItem2sfx(t) match {
        case d: DirTreeItem => expandAndCheckRecursive(d, checked)
        case i: scalafx.scene.control.TreeItem[String] => trySelectCheckbox(i.graphic.value)
      }
      )

      treeItem.check(checked)
    }

    val makeMatDirCheckboxListener = (treeItem: DirTreeItem) => new ChangeListener[java.lang.Boolean] {
      override def changed(observable: ObservableValue[_ <: lang.Boolean],
                           oldValue: lang.Boolean, newValue: lang.Boolean) = {
        expandAndCheckRecursive(treeItem, newValue)
      }
    }
}
