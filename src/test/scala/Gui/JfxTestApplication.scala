package Gui

import javafx.application.Application
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import java.net.URL
import java.util.ResourceBundle

object JfxTestApplication {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[JfxTestApplication], args: _*)
  }
}
class JfxTestApplication extends javafx.application.Application {
  override def start(primaryStage: Stage): Unit = {
    primaryStage.setTitle("HelloWorld")
    val root = new StackPane()
    primaryStage.setScene(new javafx.scene.Scene(root))
    primaryStage.sizeToScene()
    primaryStage.show()
  }
}

class JfxApplicationController extends Initializable {
  @FXML def doSomething() = {
    println("hello world")
  }
  override def initialize(url: URL, rb: ResourceBundle) = {
  }
}