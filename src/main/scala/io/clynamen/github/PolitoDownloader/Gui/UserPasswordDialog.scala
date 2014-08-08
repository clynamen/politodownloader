package io.clynamen.github.PolitoDownloader.Gui

import org.eintr.loglady.Logging

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout.{BorderPane, HBox}
import scalafx.stage.Stage

object UserPasswordDialog extends  Logging {
  var (userId, userPassword) = ("", "")
  var credentialSet = false
  def apply(exitCallback: ()=>Unit) : UserCredentials = {
    val dialog = new Stage {
      outer =>
      title = "Insert user ID and password"
      val userIdTextField =  new TextField() {
        padding = Insets(20)
        text = "user ID"
      }
      val passwordField =  new PasswordField() {
        padding = Insets(20)
        promptText = "password"
      }
      val storePasswordCheckbox = new CheckBox() {
        padding = Insets(20)
        text = "Store password for next use. (stored in plain text)"
      }

      val exitButton = new Button() {
        padding = Insets(20)
        text = "Exit"
        onAction = handle {
          MainWindow.stopApp()
          outer.close()
        }
      }

      val okButton = new Button() {
          padding = Insets(20)
          text = "Go"
          defaultButton = true
          onAction = handle {
            userId = userIdTextField.text.value
            userPassword = passwordField.text.value
            if (userId.isEmpty ||  userPassword.isEmpty) {
              InfoDialog("user ID and password fields must be filled")
            } else {
              if (storePasswordCheckbox.selected.value) {
                var config = ConfManager.readConf
                config = config.set[String]("userId", userId)
                config = config.set[String]("password", userPassword)
                ConfManager.storeConf(config)
              }
              credentialSet = true
              outer.close()
            }
          }
      }

      onCloseRequest = handle {
        if(!credentialSet) {
          MainWindow.stopApp()
        }
      }

      scene = new Scene {
        stylesheets.add("Modena.css")
        root = new BorderPane {
          padding = Insets(25)
          center = new BorderPane {
            left = userIdTextField
            right = passwordField
          }
          bottom = new BorderPane {
            left = storePasswordCheckbox
            right = new HBox(10) {
              minWidth= 200
              padding = Insets(30)
              content = List(
              exitButton,
              okButton
            )}
          }
        }
      }
    }

    // Show dialog and wait till it is closed
    log.info("Creating password dialog")
    dialog.showAndWait()
    new UserCredentials(userId, userPassword)
  }
}
