package io.clynamen.github.PolitoDownloader.Gui

import javafx.application.Platform

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import io.clynamen.github.PolitoDownloader.Client._
import org.eintr.loglady.Logging

import io.clynamen.github.PolitoDownloader.Utils.RunnableUtils.funToRunnable
import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.paint.Color

object MainWindow extends JFXApp with Logging with CheckboxTreeViewListener[ContentTreeItem]{
  val Title = "Polito Downloader"
  var actorSystem : ActorSystem = null
  var workerActor : ActorRef = null
  val DownloadFilesButtonText = "Select the files to download"
  var currentTreeId = 0

  val yearChoiceBox = new ChoiceBox[Int]() {
    items = ObservableBuffer((2001 to 2015))
    value = 2015
  }
  val matTypeChoiceBox = new ChoiceBox[ContentViewType.ContentViewType]() {
    items = ObservableBuffer(ContentViewType.Documents, ContentViewType.Videos)
    value = ContentViewType.Documents
  }

  val loadButton = new Button() {
    text = "Load"

    onAction = handle {
      updateContentsTree()
    }
  }

  val statusLine = new Label() {
    minWidth = 200
  }

  val progressIndicator = new ProgressIndicator() {
    minWidth = 20
    maxWidth = 20
    maxHeight = 20
    minHeight = 20
  }

  val downloadButton = new Button() {
    minWidth = 80
    minHeight = 20
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
    top = new HBox(4) {
      val spacer = new Region();
      content = List(spacer, matTypeChoiceBox, yearChoiceBox, loadButton)
      HBox.setHgrow(spacer, Priority.ALWAYS);
    }
    center = treeView
    bottom =
      new VBox {
        content = List(
          progressIndicator,
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
    alignment = Pos.TOP_CENTER
  }

  val rootTabPane = new TabPane()
  rootTabPane += new Tab() {
    content = downloadBorderPane
    closable = false
    text = "Files"
  }
  rootTabPane += new Tab() {
    content = new ScrollPane() {
     fitToWidth = true
     content = downloadList
    }
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
    showProgress(true)
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

  def addItem(pid: Option[ContentId], item: ContentTreeItem) = {
    itemMap.put(item.id, item)
    pid match {
      case Some(pidValue) => treeView.addItem(itemMap.get(pidValue).get, item)
      case None => treeView.addItemAtRoot(item)
    }
  }

  def expandVisitor(recursive: Boolean) = new ContentTreeItemVisitor {
    override def visit(item: DocumentTreeItem): Unit = {}
    override def visit(item: VideoTreeItem): Unit = {}
    override def visit(item: DirectoryTreeItem): Unit = {
      if(!item.contentFetched) {
        treeView.removeChildren(item)
        showProgress(true)
        workerActor ! (currentTreeId, DirReq(item.directoryInfo.id, item.directoryInfo.url, recursive))
        item.contentFetched = true
      }
    }
    override def visit(item: VideoDirectoryTreeItem) = {
      if(!item.contentFetched) {
        treeView.removeChildren(item)
        showProgress(true)
        workerActor ! (currentTreeId, VideoDirReq(item.videoDirectoryInfo.id,
                item.videoDirectoryInfo.linkData, recursive))
        item.contentFetched = true
      }
    }

  }

  def showProgress(show: Boolean) = {
    if(show) {
      progressIndicator.visible = true
      progressIndicator.progress = -1
    } else {
      progressIndicator.visible = false
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
    var downloadCount = 0
    val downloadCountVisitor = new ContentTreeItemVisitor {
      override def visit(item: DocumentTreeItem): Unit = {
        if(!item.downloaded) downloadCount += 1
      }
      override def visit(item: VideoTreeItem): Unit = {
        if(!item.downloaded) downloadCount += 1
      }
      override def visit(item: DirectoryTreeItem): Unit = {}

      override def visit(item: VideoDirectoryTreeItem): Unit = {}

    }
    treeView.checkedLeaves().foreach(i => i.visit(downloadCountVisitor))
    if(downloadCount > 0) downloadButton.text = f" Download $downloadCount files"
    else downloadButton.text = DownloadFilesButtonText
  }

  // TODO: REMOVE THIS DUPLICATION FOR GOD SAKE !!!
  // review all the class hierarchy
  def sendFileReq(item: DocumentTreeItem): Unit = {
    val downloadView = new FileDownloadView(item.info.label)
    downloadList.add(item.id, downloadView)
    treeView.setCheckable(item, false)
    item.downloaded = true
    workerActor ! FileReq(item.info.id, item.info.url, getPathForItem(item))
  }

  def sendVideoReq(item: VideoTreeItem): Unit = {
    val downloadView = new FileDownloadView(item.info.label)
    downloadList.add(item.id, downloadView)
    item.downloaded = true
    treeView.setCheckable(item, false)
    workerActor ! VideoReq(item.info.id, item.info.url,
    getPathForItem(item), item.formatChoiceBox.value.value)
  }

  def downloadFiles() = {
    val downloadVisitor = new ContentTreeItemVisitor() {
      override def visit(item: DocumentTreeItem): Unit = {
        if(!item.downloaded)
          sendFileReq(item)
      }
      override def visit(item: VideoTreeItem): Unit = {
        if(!item.downloaded)
          sendVideoReq(item)
      }
      override def visit(item: DirectoryTreeItem): Unit = {}

      override def visit(item: VideoDirectoryTreeItem): Unit = {}

    }
    treeView.checkedLeaves().foreach(f => f.visit(downloadVisitor))

    updateDownloadButton()
  }

  def getPathForItem(item: ContentTreeItem) : String = {
    treeView.parentOfItem(item) match {
      case Some(parent) => getPathForItem(parent) + "/" + parent.toString.replaceAll("/.", "")
      case None => "/"
    }
  }

  def checkItem(item: ContentTreeItem, checked: Boolean) = {
    treeView.checkItem(item, checked)
    updateDownloadButton()
  }

  def checkItemRecursively(item: ContentTreeItem, checked: Boolean) = {
    treeView.checkItemRecursively(item, checked)
    treeView.expandRecursively(item)
    updateDownloadButton()
  }

  def onFileDownloadStarted(fileId: ContentId, filename: String) = {
    Platform.runLater(funToRunnable( () => {
      setDownloadProgress(fileId, 0.1)
      statusLine.text = f"Downloading $filename"
      })
    )
  }

  def onFilePartDownloaded(fileId: ContentId, downloadedBytes: Long) = {
    // TODO: Use real file size
    Platform.runLater(funToRunnable( () => {
          val current = getDownloadedProgress(fileId)
          setDownloadProgress(fileId, Math.min(1, current + Math.abs(1 - current) * 0.00005))
        }
      )
    )
  }

  def onFileDownloaded(fileId: ContentId) = {
    val item = itemMap.get(fileId).get
    checkItem(item, true)
    setDownloadProgressCompleted(fileId, "green")
    statusLine.text = "Logged in"
  }

  def onFileDownloadError(fileId: ContentId, msg: String) = {
    setDownloadProgressCompleted(fileId, "yellow", msg)
    statusLine.text = "Logged in"
  }

  def getDownloadedProgress(fileId: ContentId) =
    downloadList.get(fileId).get.progress

  def setDownloadProgress(fileId: ContentId, percentage: Double) =
    downloadList.get(fileId).get.progress = percentage

  def setDownloadProgressCompleted(fileId: ContentId, color: String, msg: String = null) = {
    val progressBar = downloadList.get(fileId).get
    progressBar.progress = 1
    progressBar.style = f"-fx-accent: $color;"
    progressBar.applyCss()
    if(msg != null) progressBar.text_=(msg)
  }

  def updateContentsTree() = {
    fetchContentsTree(yearChoiceBox.value.value, matTypeChoiceBox.value.value)
  }
  def fetchContentsTree(year: Int, contentType: ContentViewType.ContentViewType) = {
    itemMap.empty
    treeView.empty
    currentTreeId += 1
    Platform.runLater(funToRunnable(()=> updateDownloadButton()))
    contentType match {
      case ContentViewType.Documents =>  workerActor ! (currentTreeId, CourseReq(year))
      case ContentViewType.Videos =>  workerActor ! (currentTreeId, VideoCourseReq(year))
    }
  }

  class GUIUpdateActor extends Actor {
    def receive = {
      case LoginOk(msg) => {
        updateStatusLine(msg)
        Platform.runLater(funToRunnable(()=> {
            updateDownloadButton()
            showProgress(false)
          }
        ))
        updateContentsTree()
      }
      case LoginFailed(msg) => {
        updateStatusLine(msg)
      }
      case DownloadStarted(id, filename) => {
        Platform.runLater(funToRunnable(()=> onFileDownloadStarted(id, filename)))
      }
      case PartDownloaded(id, downloadedBytes) => {
        Platform.runLater(funToRunnable(()=> onFilePartDownloaded(id, downloadedBytes)))
      }
      case DownloadCompleted(id) => {
        Platform.runLater(funToRunnable(()=> onFileDownloaded(id)))
      }
      case DownloadError(id, msg) => {
        Platform.runLater(funToRunnable(()=> onFileDownloadError(id, msg)))
      }
      case (dirInfo @ DirectoryInfo(url, label, id, pid), recursive: Boolean, treeId: Int) => {
        if (treeId == currentTreeId) {
          Platform.runLater(
            funToRunnable(() => {
                val item = new DirectoryTreeItem(dirInfo)
                addItem(dirInfo.pid, item)
                if (recursive) {
                  item.visit(expandVisitor(true))
                  checkItemRecursively(item, true)
                } else {
                  treeView.addItem(item, new PlaceholderTreeItem)
                }
                showProgress(false)
              }
            )
          )
        }
      }
      case (videoDirInfo @ VideoDirectoryInfo(linkData, label, id, pid), recursive: Boolean, treeId: Int) => {
        if (treeId == currentTreeId) {
          Platform.runLater(
            funToRunnable(() => {
              val item = new VideoDirectoryTreeItem(videoDirInfo)
              addItem(videoDirInfo.pid, item)
              if (recursive) {
                item.visit(expandVisitor(true))
                checkItemRecursively(item, true)
              } else {
                treeView.addItem(item, new PlaceholderTreeItem)
              }
              showProgress(false)
            }
            )
          )
        }
      }
      case (fileInfo @ FileInfo(url, label, id, pid, formats), recursive: Boolean, treeId: Int) => {
        if (treeId == currentTreeId) {
          Platform.runLater(funToRunnable(() => {
            val item = new DocumentTreeItem(fileInfo)
            addItem(fileInfo.pid, item)
            if (recursive)
              checkItem(item, true)
            updateDownloadButton()
            showProgress(false)
          }))
        }
      }
      case (videoFileInfo @ VideoFileInfo(label, id, pid, formats, url), recursive: Boolean, treeId: Int) => {
        if (treeId == currentTreeId) {
          Platform.runLater(funToRunnable(() => {
            val item = new VideoTreeItem(videoFileInfo)
            addItem(videoFileInfo.pid, item)
            if (recursive)
              checkItem(item, true)
            updateDownloadButton()
            showProgress(false)
          }))
        }
      }
    }
  }

  def menuBar = new MenuBar {
    menus = List(
      new Menu("File") {
        items = List(
          new Menu("Info") {
            items = List(
              new MenuItem("Version: 0.2.0 Alpha")
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

