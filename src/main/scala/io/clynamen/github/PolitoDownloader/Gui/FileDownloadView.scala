package io.clynamen.github.PolitoDownloader.Gui

import io.clynamen.github.PolitoDownloader.Client.FileInfo

import javafx.geometry.Pos
import scalafx.scene.control.{ProgressBar, Label, Skin, Control}
import scalafx.scene.layout.{Region, BorderPane}
import javafx.scene.{control => jfxc}
import scala.language.implicitConversions
import scalafx.Includes._

class FileDownloadView(labelValue: String) extends javafx.scene.layout.BorderPane {
  val progressBar = new ProgressBar()
  progressBar.alignmentInParent = Pos.BOTTOM_CENTER
  progressBar.prefWidth = 200
  val label = Label(labelValue)
  label.alignment = Pos.TOP_CENTER

  setTop(label)
  setBottom(progressBar)
  setMinWidth(200)
  setMinHeight(50)

  def progress_=(value: Double) = progressBar.progress = value
  def progress : Double = progressBar.progress.value

  def text_=(value: String) = setCenter(Label(value))
}

