package io.clynamen.github.PolitoDownloader.Test.Gui

import io.clynamen.github.PolitoDownloader.Gui.{CheckboxTreeViewListener, CheckboxTreeView}
import org.scalatest.FunSuite

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene

class CheckboxTreeViewTest extends FunSuite {


  test("it should add new child") {
    val checkboxTreeView = CheckboxTreeView[TextItemView](NullListener[TextItemView]())
    val item = new TextItemView
    checkboxTreeView.addItemAtRoot(item)
    new APP(checkboxTreeView)
//    new APP(null)
  }

}

class APP(view : CheckboxTreeView[_] = null) extends JFXApp {
  stage = new PrimaryStage {
    scene = new Scene {
      stylesheets.add("Modena.css")
//      root = view
    }
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
