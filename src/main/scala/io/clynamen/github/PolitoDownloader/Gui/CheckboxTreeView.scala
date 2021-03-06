package io.clynamen.github.PolitoDownloader.Gui

import javafx.event.EventHandler
import javafx.scene.control.TreeItem.TreeModificationEvent
import javafx.scene.{control => jfxsc}
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control.{CheckBox, TreeItem, Control, TreeView}
import scalafx.scene.Node
import scala.collection.mutable.{Map, Stack}

  class CheckboxTreeView[ItemView <: CheckboxTreeItemView] private
    (val listener : CheckboxTreeViewListener[ItemView], treeView : TreeView[ItemView] =
      new TreeView[ItemView]) extends Control(treeView) {

  private val rootItem = new TreeItem[ItemView]
  treeView.root = rootItem
  rootItem.expanded = true
  treeView.showRoot = false

  private val itemToTreeItemMap = Map[ItemView, TreeItem[ItemView]]()
  private val itemToParentItemMap = Map[ItemView, ItemView]()
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
    itemToParentItemMap.put(item, parent)
    addToTreeItem(parentTreeItem, item, treeItem)
  }

  def parentOfItem(item: ItemView) : Option[ItemView] = {
      itemToParentItemMap.get(item)
  }

  def length = itemToTreeItemMap.size

  def checkItem(item: ItemView, checked: Boolean) = {
     setChecked(getTreeItemOrThrowNonExistent(item), checked)
  }

  def setCheckable(item: ItemView, checkable: Boolean) = {
    checkboxOf(item).setDisable(!checkable)
  }

  private def checkboxOf(item: ItemView) : CheckBox = {
    checkboxOf(getTreeItemOrThrowNonExistent(item))
  }
  private def checkboxOf(item: TreeItem[ItemView]) : CheckBox = {
      item.graphic.
        value.asInstanceOf[HCoupleBox[javafx.scene.control.CheckBox, javafx.scene.control.Control]].left
  }

  def expandRecursively(item: ItemView) = {
    val treeItem = getTreeItemOrThrowNonExistent(item)
    expandTreeItemRecursively(treeItem)
  }

  private def expandTreeItemRecursively(item: TreeItem[ItemView]) : Unit = {
    item.expanded = true
    item.children.foreach(i=> expandTreeItemRecursively(i))
  }

  def checkItemRecursively(item: ItemView, checked: Boolean) = {
    recursiveCheck(getTreeItemOrThrowNonExistent(item), checked)
  }

  def isChecked(item: ItemView) : Boolean = getChecked(getTreeItemOrThrowNonExistent(item))
  def hasChildren(item: ItemView) : Boolean = {
    getTreeItemOrThrowNonExistent(item).children.size > 0
  }

  def removeChildren(item: ItemView) = {
    removeTreeItemChildren(getTreeItemOrThrowNonExistent(item))
  }

  def removeTreeItemChildren(item: TreeItem[ItemView]) : Unit = {
    item.children.foreach(i=>removeTreeItemChildren(i))
    val toRemove = item.children.toList
    toRemove.foreach(i=> removeTreeItem(item, i))
  }

  def removeTreeItem(parent: TreeItem[ItemView], item: TreeItem[ItemView]) = {
    itemCount -= 1
    itemToTreeItemMap.remove(item.getValue)
    itemToParentItemMap.remove(item.getValue)
    parent.children.remove(item)
  }

  def empty : Unit = {
    removeTreeItemChildren(rootItem)
  }

  def getTreeItemOrThrowNonExistent(item: ItemView) = itemToTreeItemMap.getOrElse(item, throw new Exception("Non existent item"))

  def checkedLeaves() = getLeaves(v=>checkboxOf(v).selected.value)
  def uncheckedLeaves() = getLeaves(v=>(!(checkboxOf(v).selected.value)))
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
            if (treeItem.children.length > 0  || (treeItem eq rootItem) || !filterItem(treeItem) ) {
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
    val hCoupleBox :HCoupleBox[javafx.scene.control.CheckBox, javafx.scene.Node] =
      new HCoupleBox(checkbox, item.graphic.getOrElse[javafx.scene.Node](new EmptyControl()))
    treeItem.graphic = hCoupleBox
    checkbox.onAction = (event : ActionEvent) => {
      recursiveCheck(treeItem, checkbox.selected())
      listener.onItemCheckedByUser(item, checkbox.selected())
    }
    treeItem.addEventHandler(jfxsc.TreeItem.branchExpandedEvent[String], new EventHandler[TreeModificationEvent[String]] {
      override def handle(event: TreeModificationEvent[String]): Unit = {
        listener.onBranchExpanded(item)
        treeItem.removeEventHandler(jfxsc.TreeItem.branchExpandedEvent[String], this)
      }
    })
    treeItem
  }

   private def recursiveCheck(item: TreeItem[ItemView], checked : Boolean): Unit = {
     setChecked(item, checked)
     item.children.foreach(c => recursiveCheck(c, checked))
   }

   private def setChecked(item: TreeItem[ItemView], value : Boolean) =
     checkboxOf(item).selected.value = value

   private def getChecked(item: TreeItem[ItemView]) =
     checkboxOf(item).selected.value

}

object CheckboxTreeView {

  def apply[ItemView <: CheckboxTreeItemView]
    (listener: CheckboxTreeViewListener[ItemView]) :
      CheckboxTreeView[ItemView] = {
    new CheckboxTreeView[ItemView](listener)
  }


}

trait CheckboxTreeViewListener[ItemView] {

  def onItemCheckedByUser(item: ItemView, checked: Boolean)
  def onBranchExpanded(item: ItemView)

}

trait CheckboxTreeItemView {
  def graphic : Option[javafx.scene.Node]
}