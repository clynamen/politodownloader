package io.clynamen.github.PolitoDownloader.Gui

import scalafx.geometry.{HPos, Orientation}
import scalafx.scene.layout.FlowPane
import javafx.scene.Node
import scala.collection.mutable.Map

class DownloadListView[K, V <: Node] extends FlowPane(Orientation.VERTICAL) {
  val keyToItemMap = Map[K,V]()
  columnHalignment = HPos.CENTER

  def get(k: K) : Option[V] = {
    keyToItemMap.get(k)
  }

  def add(k: K, v: V) = {
    keyToItemMap.put(k, v)
    content.add(v)
  }

}
