package io.clynamen.github.PolitoDownloader.Gui

import javafx.scene.{control => jfxsc}
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control.{CheckBox, TreeItem, Control, TreeView}
import scala.collection.mutable.{Map, Stack}

  class CheckboxTreeView[ItemView] private
    (val listener : CheckboxTreeViewListener[ItemView], treeView : TreeView[ItemView] =
      new TreeView[ItemView]) extends Control(treeView) {

  private val rootItem = new TreeItem[ItemView]
  treeView.root = rootItem

  private val itemToTreeItemMap = Map[ItemView, TreeItem[ItemView]]()
  private var itemCount = 0

  def addItemAtRoot(item: ItemView) : Unit = {
    val treeItem = createTreeItem(item)
    addToTreeItem(treeView.root(), item, treeItem)
  }

  def addItem(parent: ItemView, item: ItemView) = {
    val treeItem = createTreeItem(item)
    val parentTreeItem = itemToTreeItemMap.getOrElse(parent, {
      throw new Exception("Trying to add item to non existing parent")
    })
    addToTreeItem(parentTreeItem, item, treeItem)
  }

  def length = itemCount

  def checkItem(item: ItemView, checked: Boolean) = {
     setChecked(getTreeItemOrThrowNonExistent(item), checked)
  }

  def checkItemRecursively(item: ItemView, checked: Boolean) = {
    recursiveCheck(getTreeItemOrThrowNonExistent(item), checked)
  }

  def isChecked(item: ItemView) : Boolean = getChecked(getTreeItemOrThrowNonExistent(item))

  def getTreeItemOrThrowNonExistent(item: ItemView) = itemToTreeItemMap.getOrElse(item, throw new Exception("Non existent item"))

  def checkedLeaves() = getLeaves(v=>(v.graphic.value.asInstanceOf[jfxsc.CheckBox]).selected.value)
  def uncheckedLeaves() = getLeaves(v=>(!(v.graphic.value.asInstanceOf[jfxsc.CheckBox]).selected.value))
  def leaves() = getLeaves()

  private def getLeaves(filterItem : (TreeItem[ItemView]) => (Boolean) = (_) => true) : Iterable[ItemView] = {

    new Iterable[ItemView] {

      def iterator = new Iterator[ItemView] {
        val stack = Stack[scalafx.scene.control.TreeItem[ItemView]]()
        var nextItemView : Option[ItemView] = None
        stack.push(rootItem)

        def findNext(): Option[ItemView] = {
          while(stack.length > 0) {
            val treeItem = stack.pop()
            if (treeItem.children.length > 0 || !filterItem(treeItem) || (treeItem eq rootItem) ) {
//              stack.pushAll(treeItem.children.toList)
              for ( c <- treeItem.children) stack.push(c)
            } else {
              return Some(treeItem.getValue)
            }
          }
          None
        }

        override def hasNext: Boolean = {
          if(nextItemView.isEmpty) {
            findNext match {
              case None => false
              case v @ Some(_) => {
                nextItemView = v
                true
              }
            }
          } else false
        }

        override def next(): ItemView = {
          if(nextItemView.isDefined ) {
            val v = nextItemView.get
            nextItemView = None
            v
          }
          else {
            if(hasNext) {
              val v = nextItemView.get
              nextItemView = None
              nextItemView.get
            }
            else
              throw new RuntimeException("No more items")
          }
        }

      }
    }

  }

  private def addToTreeItem(parent: TreeItem[ItemView], child: ItemView, childTreeItem: TreeItem[ItemView]) = {
    parent.children.add(childTreeItem)
    itemToTreeItemMap.put(child, childTreeItem)
    itemCount += 1
  }

  private def createTreeItem(item: ItemView) = {
    val treeItem = new TreeItem[ItemView](item)
    val checkbox = new CheckBox
    treeItem.graphic = checkbox
    checkbox.onAction = (event : ActionEvent) => {
      recursiveCheck(treeItem, checkbox.selected())
      listener.onItemCheckedByUser(item, checkbox.selected())
    }
    treeItem
  }

   private def recursiveCheck(item: TreeItem[ItemView], checked : Boolean): Unit = {
     setChecked(item, checked)
     item.children.foreach(c => recursiveCheck(c, checked))
   }

   private def setChecked(item: TreeItem[ItemView], value : Boolean) =
     item.graphic.value.asInstanceOf[jfxsc.CheckBox].selected.value = value

   private def getChecked(item: TreeItem[ItemView]) =
      item.graphic.value.asInstanceOf[jfxsc.CheckBox].selected.value

}

object CheckboxTreeView {

  def apply[ItemView]
    (listener: CheckboxTreeViewListener[ItemView]) :
      CheckboxTreeView[ItemView] = {
    new CheckboxTreeView[ItemView](listener)
  }

}

trait CheckboxTreeViewListener[ItemView] {

  def onItemCheckedByUser(item: ItemView, checked: Boolean)

}
