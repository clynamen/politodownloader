package io.clynamen.github.PolitoDownloader.Gui

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout.BorderPane
import scalafx.stage.Stage

object InfoDialog {
  def apply(infoText : String) = {
    val dialog = new Stage {
      outer =>
      title = infoText

      val okButton = new Button() {
        padding = Insets(25)
        text = "Ok"
        onAction = handle {
          outer.close()
        }
        alignment = Pos.BOTTOM_RIGHT
      }

      scene = new Scene {
        stylesheets.add("Modena.css")
        root = new BorderPane {
          padding = Insets(25)
          center = new Label() {
            padding = Insets(25)
            text = infoText
          }
          bottom = okButton
        }
      }
    }
    // Show dialog and wait till it is closed
    dialog.showAndWait()
  }
}
