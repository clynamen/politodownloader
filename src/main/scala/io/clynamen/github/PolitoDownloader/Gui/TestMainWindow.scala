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
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout.{BorderPane, HBox, Priority, VBox}


object TestMainWindow extends JFXApp with Logging {
  val checkboxTreeView = CheckboxTreeView[TextItemView, TextItemViewVisitor](NullListener[TextItemView]())
  val item = new TextItemView
  checkboxTreeView.addItemAtRoot(item)
  stage = new PrimaryStage {
    scene = new Scene {
      stylesheets.add("Modena.css")
      root = checkboxTreeView
    }
  }
}


class TextItemView extends CheckboxTreeViewItemView[TextItemViewVisitor] {
  override def visit(visitor: TextItemViewVisitor): Unit = {}
}

class TextItemViewVisitor {

}

class NullListener[ItemView] extends CheckboxTreeViewListener[ItemView] {
  override def onItemCheckedByUser(item: ItemView, checked: Boolean): Unit = {}
}

object NullListener {
  def apply[T]() = new NullListener[T]()
}
