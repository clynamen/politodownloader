package io.clynamen.github.PolitoDownloader.Utils

import scala.io.Source
import java.net.URI
import java.net.URL
import scala.language.implicitConversions

object StringUtils {

  def isNullOrEmpty(s: String) = s match {
    case "" => true
    case null => true
    case _ => false
  }

}

object FileUtils {
  def slurp(s: String) : String = {
    Source.fromFile(s).mkString
  }
  def slurp(s: URI) : String = {
    Source.fromFile(s).mkString
  }
  def slurp(s: URL) : String = {
    slurp(s.toURI)
  }
}
object RunnableUtils {
  // TODO: is this in std library?
  implicit def funToRunnable(fun: () => Unit) = new Runnable() {
    def run() = fun()
  }
}