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
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout._


object MainWindow extends JFXApp with Logging with CheckboxTreeViewListener[ContentTreeItem]{
  val Title = "Polito Downloader"
  var actorSystem : ActorSystem = null
  var workerActor : ActorRef = null
  val DownloadFilesButtonText = "Select the files to download"

  val statusLine = new Label() {
    minWidth = 200
    maxWidth = 200
  }

  val downloadButton = new Button() {
    minWidth = 100
    minHeight = 30
    text = "Waiting..."
    onAction = handle {
      downloadFiles()
    }
  }

  val treeView = CheckboxTreeView[ContentTreeItem](this)
  val conf = ConfManager.readConf
  treeView.minWidth = 300
  treeView.minHeight = 200
  treeView.hgrow = Priority.ALWAYS

  val downloadBorderPane = new BorderPane {
    center = treeView
    bottom =
      new VBox {
        content = List(
          statusLine,
          new HBox(20) {
            content = List(
              downloadButton
            )
            alignment = Pos.BOTTOM_RIGHT
            padding = Insets(20)
          }
        )
        padding = Insets(20)
      }
  }

  val downloadList = new DownloadListView[ContentId, FileDownloadView]() {
    margin = Insets(20)
  }

  val rootTabPane = new TabPane()
  rootTabPane += new Tab() {
    content = downloadBorderPane
    closable = false
    text = "Files"
  }
  rootTabPane += new Tab() {
    content = downloadList
    closable = false
    text = "Download List"
  }

  val rootPane = new BorderPane {
    top = menuBar
    center = rootTabPane
  }


  stage = new PrimaryStage {
    onCloseRequest = handle { cleanup() }
    title = Title
    scene = new Scene {
      // TODO: not needed in production, but needed under sbt run
      stylesheets.add("Modena.css")
      root = rootPane
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

  var itemMap = scala.collection.mutable.Map[ContentId, ContentTreeItem]()
  var expandedSet = scala.collection.mutable.Set[ContentId]()

  def addItem(pid: Option[ContentId], item: ContentTreeItem) = {
    itemMap.put(item.id, item)
    pid match {
      case Some(pidValue) => treeView.addItem(itemMap.get(pidValue).get, item)
      case None => treeView.addItemAtRoot(item)
    }
  }

  def expandVisitor(recursive: Boolean) = new ContentTreeItemVisitor {
    override def visit(item: DocumentTreeItem): Unit = {}
    override def visit(item: DirectoryTreeItem): Unit = {
      if(!item.contentFetched) {
        treeView.removeChildren(item)
        workerActor ! DirReq(item.directoryInfo.id, item.directoryInfo.url, recursive)
        item.contentFetched = true
      }
    }
  }

  override def onItemCheckedByUser(item: ContentTreeItem, checked: Boolean) = {
    item.visit(expandVisitor(checked))
    checkItemRecursively(item, checked)
  }

  override def onBranchExpanded(item: ContentTreeItem): Unit = {
    item.visit(expandVisitor(false))
  }

  def updateStatusLine(msg : String) = {
    import io.clynamen.github.PolitoDownloader.Utils.RunnableUtils.funToRunnable
    Platform.runLater(funToRunnable(() => statusLine.text = msg))
  }

  def setFileDownloadStatus(downloadDetails : FileInfo, toDownload : Boolean) = {
    updateDownloadButton()
  }

  def updateDownloadButton() {
    val downloadCount = treeView.checkedLeaves().size
    if(downloadCount > 0) downloadButton.text = f" Download $downloadCount files"
    else downloadButton.text = DownloadFilesButtonText
  }

  def sendFileReq(item: DocumentTreeItem): Unit = {
    workerActor ! FileReq(item.info.id, item.info.url, getPathForItem(item))
    val downloadView = new FileDownloadView(item.info)
    downloadList.content.add(downloadView)
  }

  def downloadFiles() = {
    val downloadVisitor = new ContentTreeItemVisitor() {
      override def visit(item: DocumentTreeItem): Unit = {
        sendFileReq(item)
      }
      override def visit(item: DirectoryTreeItem): Unit = {}
    }
    treeView.checkedLeaves().foreach(f => f.visit(downloadVisitor))

    updateDownloadButton()
  }

  def getPathForItem(item: ContentTreeItem) : String = {
    treeView.parentOfItem(item) match {
      case Some(parent) => getPathForItem(parent) + "/" + parent
      case None => "/"
    }
  }

  def checkItem(item: ContentTreeItem, checked: Boolean) = {
    treeView.checkItem(item, checked)
    updateDownloadButton()
  }

  def checkItemRecursively(item: ContentTreeItem, checked: Boolean) = {
    treeView.checkItemRecursively(item, checked)
    updateDownloadButton()
  }

  def onFileDownloaded(fileId: ContentId, msg : String) = {
    updateStatusLine(msg)
    val item = itemMap.get(fileId).get
    checkItem(item, true)
    treeView.setCheckable(item, false)
    setDownloadProgress(fileId, 1)
  }

  def setDownloadProgress(fileId: ContentId, percentage: Double) = {
    downloadList.get(fileId).get.progress = percentage
  }

  class GUIUpdateActor extends Actor {
    import io.clynamen.github.PolitoDownloader.Utils.RunnableUtils.funToRunnable
    def receive = {
      case LoginOk(msg) => {
        updateStatusLine(msg)
        Platform.runLater(funToRunnable(()=> updateDownloadButton()))
        workerActor ! CourseReq(2014)
      }
      case LoginFailed(msg) => {
        updateStatusLine(msg)
      }
      case FileDownloaded(id, msg) => {
        Platform.runLater(funToRunnable(()=> onFileDownloaded(id, msg)))
      }
      case (dirInfo @ DirectoryInfo(url, label, id, pid), recursive: Boolean) => {
        Platform.runLater(funToRunnable(() => {
          val item = new DirectoryTreeItem(dirInfo)
          addItem(dirInfo.pid, item)
          if (recursive) {
            treeView.expandRecursively(item)
            checkItemRecursively(item, true)
          } else {
            treeView.addItem(item, new PlaceholderTreeItem)
          }
        })
        )
      }
      case (fileInfo @ FileInfo(url, label, id, pid, formats), recursive: Boolean) => {
        Platform.runLater(funToRunnable(() => {
          val item = new DocumentTreeItem(fileInfo)
          addItem(fileInfo.pid, item)
          if (recursive)
            checkItem(item, true)
          updateDownloadButton()
        }))
      }
    }
  }

  def menuBar = new MenuBar {
    menus = List(
      new Menu("File") {
        items = List(
          new Menu("Info") {
            items = List(
              new MenuItem("Version: 0.1.0 Alpha")
            )
          },
          new MenuItem("reset configuration and close") {
            onAction = handle {
              ConfManager.deleteConf
              stopApp()
            }
          },
          new MenuItem("close") {
            onAction = handle {
              stopApp()
            }
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

