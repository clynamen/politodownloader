package io.clynamen.github.PolitoDownloader.Gui

import javafx.scene.{control => jfxc}
import javafx.scene.layout.HBox
import scalafx.Includes._

class HCoupleBox[L <: javafx.scene.Node, R <: javafx.scene.Node] (val left: L, val right: R) extends HBox(4) {
  getChildren.add(left)
  getChildren.add(right)
}
