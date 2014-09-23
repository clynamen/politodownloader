package io.clynamen.github.PolitoDownloader.Utils

import scala.language.implicitConversions

object StringUtils {

  def isNullOrEmpty(s: String) = s match {
    case "" => true
    case null => true
    case _ => false
  }

}

object RunnableUtils {
  // TODO: is this in std library?
  implicit def funToRunnable(fun: () => Unit) = new Runnable() {
    def run() = fun()
  }
}