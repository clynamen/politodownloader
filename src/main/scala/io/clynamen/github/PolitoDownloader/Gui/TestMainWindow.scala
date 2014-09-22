package io.clynamen.github.PolitoDownloader.Gui

import javafx.application.Platform
import javafx.scene.{control => jfxsc}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import io.clynamen.github.PolitoDownloader.Client._
import org.eintr.loglady.Logging

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.event.ActionEvent
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout.{BorderPane, HBox, Priority, VBox}


object TestMainWindow extends JFXApp with Logging {
  val checkboxTreeView = CheckboxTreeView[GItemView, TextItemViewVisitor](NullListener[GItemView]())
  val item = new TextItemView
  checkboxTreeView.addItemAtRoot(item)
  checkboxTreeView.addItem(item, new TextItemView)
  checkboxTreeView.addItem(item, new TextItemView)
  checkboxTreeView.addItem(item, new IconItemView)
  checkboxTreeView.addItem(item, new IconItemView)
  checkboxTreeView.addItem(item, new TextItemView)
  val button = new Button() {
    onAction = (e : ActionEvent) => {
      var n : Integer = 0
      for ( l <- checkboxTreeView.checkedLeaves) {
        n = n+1
      }
      this.text = "changed = " + n
    }
  }
  stage = new PrimaryStage {
    scene = new Scene {
      stylesheets.add("Modena.css")
      root = new VBox {
        content = List(checkboxTreeView , button)
      }
    }
  }
}

trait GItemView extends CheckboxTreeViewItemView[TextItemViewVisitor] {

}

class TextItemView extends GItemView {
  override def visit(visitor: TextItemViewVisitor): Unit = visitor.visit(this)
}

class IconItemView extends GItemView {
  override def visit(visitor: TextItemViewVisitor): Unit = visitor.visit(this)
}

class TextItemViewVisitor {

  def visit(i : TextItemView): Unit = {

  }

  def visit(i : IconItemView): Unit = {

  }

}

class NullListener[ItemView] extends CheckboxTreeViewListener[ItemView] {
  override def onItemCheckedByUser(item: ItemView, checked: Boolean): Unit = {
    println("clicked: " + item + " " + item.getClass)
  }
}

object NullListener {
  def apply[T]() = new NullListener[T]()
}
