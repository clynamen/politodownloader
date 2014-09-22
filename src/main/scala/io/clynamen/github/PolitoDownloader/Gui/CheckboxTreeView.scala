package io.clynamen.github.PolitoDownloader.Gui

import javafx.scene.{control => jfxsc}
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control.{CheckBox, TreeItem, Control, TreeView}
import scala.collection.mutable.{Map, Stack}

//class CheckboxTreeView[ItemView <: CheckboxTreeViewItemView[CheckboxTreeViewVisitor],
//                        CheckboxTreeViewVisitor] private
//  (val listener : CheckboxTreeViewListener[ItemView], treeView : TreeView[ItemView] =
//    new TreeView[ItemView]) extends Control(treeView) {
  class CheckboxTreeView[ItemView <: CheckboxTreeViewItemView[CheckboxTreeViewVisitor],
  CheckboxTreeViewVisitor] private
  (val listener : CheckboxTreeViewListener[ItemView], treeView : TreeView[ItemView] =
  new TreeView[ItemView]) extends Control(treeView) {

  private val rootItem = new TreeItem[ItemView]
  treeView.root = rootItem

  private val itemToTreeItemMap = Map[ItemView, TreeItem[ItemView]]()

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
  }

  private def createTreeItem(item: ItemView) = {
    val treeItem = new TreeItem[ItemView](item)
    val checkbox = new CheckBox
    treeItem.graphic = checkbox
    checkbox.onAction = (event : ActionEvent) => {
      listener.onItemCheckedByUser(item, checkbox.selected())
    }
    treeItem
  }

}

object CheckboxTreeView {

  def apply[ItemView <: CheckboxTreeViewItemView[CheckboxTreeViewVisitor], CheckboxTreeViewVisitor]
    (listener: CheckboxTreeViewListener[ItemView]) :
      CheckboxTreeView[ItemView, CheckboxTreeViewVisitor] = {
    new CheckboxTreeView[ItemView, CheckboxTreeViewVisitor](listener)
  }

}

trait CheckboxTreeViewItemView[CheckboxTreeViewVisitor] {

  def visit(visitor: CheckboxTreeViewVisitor)

}

trait CheckboxTreeViewListener[ItemView] {

  def onItemCheckedByUser(item: ItemView, checked: Boolean)

}
