package io.clynamen.github.PolitoDownloader.Gui

import org.eintr.loglady.Logging

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.geometry.{Pos, Insets}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout.{BorderPane, VBox, HBox}
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
        minWidth = 150
        text = "user ID"
      }

      val passwordField =  new PasswordField() {
        padding = Insets(20)

        minWidth = 150
        promptText = "password"
      }

      val storePasswordCheckbox = new CheckBox() {
        padding = Insets(20)
        disable = true
        minWidth = 200
        text = "Store password for next use. (stored in plain text) (broken in scala 2.11)"
        textOverrun = OverrunStyle.ELLIPSIS
        wrapText = true
      }

      val exitButton = new Button() {
        padding = Insets(20)
        minWidth = 50
        text = "Exit"
        onAction = handle {
          MainWindow.stopApp()
          outer.close()
        }
      }

      val okButton = new Button() {
          padding = Insets(20)
          text = "Go"
          minWidth = 50
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
        minWidth = 550
        minHeight = 200
        resizable = false

        root = new VBox {
          content = List(
            new HBox(20) {
              padding = Insets(20)
              content = List(userIdTextField, passwordField)
              alignment.value = Pos.CENTER
            },
            new VBox(10) {
              maxHeight = 100
              content = List(storePasswordCheckbox,
                new HBox(20) {
                  content = List(
                    exitButton,
                    okButton
                  )
                  padding = Insets(20)
                  alignment.value = Pos.BOTTOM_RIGHT
                }
              )
            }
          )
          alignment.value = Pos.CENTER
        }
      }
    }

    // Show dialog and wait till it is closed
    log.info("Creating password dialog")
    dialog.showAndWait()
    new UserCredentials(userId, userPassword)
  }
}
