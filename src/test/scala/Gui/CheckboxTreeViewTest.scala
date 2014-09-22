package Gui

import javafx.application.Platform

import io.clynamen.github.PolitoDownloader.Gui.{CheckboxTreeView, CheckboxTreeViewListener}
import org.jemmy.fx.AppExecutor
import org.scalatest.FunSuite

import scala.collection.mutable.Set

/**
 * FIXME: Fix the initialization of the javafx environment. Currently the enviroment is created
 * in the first test and shutdown in the last
 */

class CheckboxTreeViewTest extends FunSuite {

  implicit class TextItemViewTest(val a : TextItemView) {
    /**
     * Append b to the children of this item.
     * @param b
     * @param tree
     * @return
     */
    def <+ (b: TextItemView)(implicit tree : CheckboxTreeView[TextItemView]) : Unit = {
      tree.addItem(a, b)
    }
  }

  test("it should add new child") {
    AppExecutor.executeNoBlock(classOf[JfxTestApplication])
    val checkboxTreeView = CheckboxTreeView[TextItemView](NullListener[TextItemView]())
    val item = new TextItemView
    assert(checkboxTreeView.length === 0)
    checkboxTreeView.addItemAtRoot(item)
    assert(checkboxTreeView.length === 1)
  }

  test("leaves() should iterate on all the leaves") {
    implicit val checkboxTreeView = CheckboxTreeView[TextItemView](NullListener[TextItemView]())
    val a = new TextItemView
    val b = new TextItemView
    val aa = new TextItemView
    val ab = new TextItemView
    val ac = new TextItemView
    val ba = new TextItemView
    val aaa = new TextItemView
    val aba = new TextItemView
    val abb = new TextItemView
    val abc = new TextItemView
    checkboxTreeView.addItemAtRoot(a)
    checkboxTreeView.addItemAtRoot(b)
    a <+ aa
    a <+ ab
    a <+ ac
    b <+ ba
    aa <+ aaa
    ab <+ aba
    ab <+ abb
    ab <+ abc
    val leaves = Set(ac, ba, aaa, aba, abb, abc)
    for ( l <- checkboxTreeView.leaves) leaves.remove(l)
    assert(leaves.size === 0)
  }

  test("checkedLeaves() should iterate on all the checked leaves") {
    implicit val checkboxTreeView = CheckboxTreeView[TextItemView](NullListener[TextItemView]())
    val a = new TextItemView
    val b = new TextItemView
    val aa = new TextItemView
    val ab = new TextItemView
    val ac = new TextItemView
    val ba = new TextItemView
    val aaa = new TextItemView
    val aba = new TextItemView
    val abb = new TextItemView
    val abc = new TextItemView
    checkboxTreeView.addItemAtRoot(a)
    checkboxTreeView.addItemAtRoot(b)
    a <+ aa
    a <+ ab
    a <+ ac
    b <+ ba
    aa <+ aaa
    ab <+ aba
    ab <+ abb
    ab <+ abc
    val checkedLeaves = Set(ac, aba, abb)
    checkedLeaves.foreach(l => checkboxTreeView.checkItem(l, true))
    for ( l <- checkboxTreeView.leaves) checkedLeaves.remove(l)
    assert(checkedLeaves.size === 0)
  }

  test("children should be checked when parent is checked by user") {
    //AppExecutor.executeNoBlock(classOf[JfxApplication])
    implicit val checkboxTreeView = CheckboxTreeView[TextItemView](NullListener[TextItemView]())
    val item = new TextItemView
    val child1 = new TextItemView
    val child2 = new TextItemView
    checkboxTreeView.addItemAtRoot(item)
    item <+ child1
    item <+ child2
    checkboxTreeView.checkItemRecursively(item, true)
    assert(checkboxTreeView.isChecked(child1) === true)
    assert(checkboxTreeView.isChecked(child2) === true)
  }

  test("children should be checked recursively when parent is checked by user") {
    //AppExecutor.executeNoBlock(classOf[JfxApplication])
    implicit val checkboxTreeView = CheckboxTreeView[TextItemView](NullListener[TextItemView]())
    val item = new TextItemView
    val child1 = new TextItemView
    val child2 = new TextItemView
    checkboxTreeView.addItemAtRoot(item)
    item <+ child1
    child1 <+ child2
    checkboxTreeView.checkItemRecursively(item, true)
    assert(checkboxTreeView.isChecked(child1) === true)
    assert(checkboxTreeView.isChecked(child2) === true)
    Platform.exit()
  }


}

class TextItemView {
  def visit(visitor: TextItemViewVisitor): Unit = {}
}

class TextItemViewVisitor {

}

class NullListener[ItemView] extends CheckboxTreeViewListener[ItemView] {
  override def onItemCheckedByUser(item: ItemView, checked: Boolean): Unit = {}
}

object NullListener {
  def apply[T]() = new NullListener[T]()
}

