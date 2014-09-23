package io.clynamen.github.PolitoDownloader.Gui

import io.clynamen.github.PolitoDownloader.Client.FileInfo

import scalafx.scene.control.{ProgressBar, Label, Skin, Control}
import scalafx.scene.layout.{Region, BorderPane}
import javafx.scene.{control => jfxc}

class FileDownloadView(fileInfo: FileInfo) extends jfxc.Control {
  val progressBar = new ProgressBar()
  val root = new BorderPane() {
    top = Label(fileInfo.label)
    bottom = progressBar
  }
  getChildren.add(root)

  def progress_=(value: Double) = progressBar.progress = value
  def progress : Double = progressBar.progress.value
}

