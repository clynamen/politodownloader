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
import scalafx.scene.layout.{BorderPane, HBox, VBox}


object MainWindow extends JFXApp with Logging {
  val Title = "Polito Downloader"
  var actorSystem : ActorSystem = null
  var workerActor : ActorRef = null
  var toDownloadSet = scala.collection.mutable.Set[FileDownloadDetails]()
  val DownloadFilesButtonText = "Select files to download"

  def fetchDir(pid: Int, url: String, recursive: Boolean) = {
    workerActor ! DirReq(pid, url, recursive)
  }

  def fetchFile(url: String, fileName: String) = {
    workerActor ! FileReq(url, fileName)
  }

  val statusLine = new Label() {
    minWidth = 200
    maxWidth = 200
  }

  val rootTreeItem =
    new TreeItem[String]("invisible root")

  val downloadButton = new Button() {
    minWidth = 200
    minHeight = 50
    maxWidth = 100
    maxHeight= 50
    text = DownloadFilesButtonText
    onAction = handle {
      downloadFiles()
    }
  }

  val treeView = new TreeView[String] {
    val conf = ConfManager.readConf
    minWidth = 700
    minHeight = 500
    showRoot = false
    root = rootTreeItem
  }

  stage = new PrimaryStage {
    onCloseRequest = handle { cleanup() }
    title = Title
    scene = new Scene {
      // TODO: not needed in production, but needed under sbt run
      stylesheets.add("Modena.css")
      root = new BorderPane {
        top = menuBar
        center = new HBox( ) {
            content = List(
              treeView,
              new VBox {
                content = List(
                statusLine,
                downloadButton
              )
            }
          )
        }
      }
    }
  }

  initGui()

  def initGui() = {
    val userCredentials = getUserIdAndPassword
    val (newSystem, newActor) = createActorSystemWithWorkerActor(userCredentials.userId, userCredentials.password)
    actorSystem = newSystem
    workerActor = newActor
    updateStatusLine("Logging in... (it takes some time)")
    workerActor ! LoginReq()
  }

  def getUserIdAndPassword : UserCredentials = {
    if(ConfManager.isFirstInit) {
       UserPasswordDialog(MainWindow.stopApp)
    } else {
      val conf = ConfManager.readConf
      val (userId :String, password : String) = (conf[String]("userId", ""), conf[String]("password", ""))
      if(userId.isEmpty || password.isEmpty)
        UserPasswordDialog(MainWindow.stopApp)
      else
        new UserCredentials(userId, password)
    }
  }

  def createActorSystemWithWorkerActor(userId : String, password: String): (ActorSystem, ActorRef) = {
    val system = ActorSystem()

    val guiUpdateActor = system.actorOf(Props[GUIUpdateActor], name = "guiUpdateActor")
    val workerActor = system.actorOf((Props(new ClientActor(guiUpdateActor, userId, password))), name = "workerActor")

    (system, workerActor)
  }

  var itemMap = scala.collection.mutable.Map[Int, MaterialTreeItem]()

  def addItem(pid: Int, id: Int, item: MaterialTreeItem) = {
    itemMap.put(id, item)
    val parent: TreeItem[String] = itemMap.get(pid) match {
      case Some(parentItem: scalafx.scene.control.TreeItem[String]) => parentItem
      case None => rootTreeItem
    }
    parent.children.add(item)
  }

  def updateStatusLine(msg : String) = {
    import io.clynamen.github.PolitoDownloader.Utils.RunnableUtils.funToRunnable
    Platform.runLater(funToRunnable(() => statusLine.text = msg))
  }

  def setFileDownloadStatus(downloadDetails : FileDownloadDetails, toDownload : Boolean) = {
    if(!toDownload && toDownloadSet.contains(downloadDetails))
      toDownloadSet.remove(downloadDetails)
    else {
      toDownloadSet.add(downloadDetails)
    }
    updateDownloadButton()
  }

  def updateDownloadButton() {
    val downloadCount = toDownloadSet.size
    if(downloadCount > 0) downloadButton.text = f" Download $downloadCount files"
    else downloadButton.text = DownloadFilesButtonText
  }

  def downloadFiles() = {
    toDownloadSet.foreach(d => {
      workerActor ! FileReq(d.url, d.path)
    })
    toDownloadSet.clear()
    toDownloadSet = collection.mutable.Set[FileDownloadDetails]()
    itemMap.values.foreach( v=>v match {
      case t : FileTreeItem => if(t.checkbox.selected.value) t.checkable(false)
      case _ => ;
    })
    updateDownloadButton()
  }

  class GUIUpdateActor extends Actor {
    import io.clynamen.github.PolitoDownloader.Utils.RunnableUtils.funToRunnable
    def receive = {
      case LoginOk(msg) => {
        updateStatusLine(msg)
        workerActor ! ClassesReq()
      }
      case LoginFailed(msg) => {
        updateStatusLine(msg)
      }
      case FileDownloaded(msg) => {
        updateStatusLine(msg)
      }
      case MDir(name, pid, id, url, recursive) => {
        Platform.runLater(funToRunnable(() => {
          val item = new DirTreeItem(id, name, url, fetchDir)
          addItem(pid, id, item)
          if (recursive) {
            item.expandItem(true)
            item.checkAllRecursive(true)
          }
        })
        )
      }
      case MFile(name, pid, id, url, recursive) => {
        Platform.runLater(funToRunnable(() => {
          val item = new FileTreeItem(name, id, url, setFileDownloadStatus)
          addItem(pid, id, item)
          if (recursive) item.check(true)
        }))
      }
    }
  }

  def menuBar = new MenuBar {
    menus = List(
      new Menu("File") {
        items = List(
          new MenuItem("reset and close") {
            onAction = handle {
              ConfManager.deleteConf
              stopApp()
            }
          },
          new Menu("Info") {
            items = List(
              new MenuItem("Version: 0.1.0 Alpha")
            )
          }
        )
      }
    )
  }

  override def stopApp() = {
    cleanup()
    super.stopApp()
    Platform.exit()
  }

  def cleanup(): Unit = {
    log.info("Cleaning up...")
    log.info("killing actors...")
    if(actorSystem != null) actorSystem.shutdown()
  }

}
