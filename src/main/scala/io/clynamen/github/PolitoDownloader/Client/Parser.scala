package io.clynamen.github.PolitoDownloader.Client

import org.eintr.loglady.Logging
import org.filippodeluca.ssoup.SSoup
import org.filippodeluca.ssoup.SSoup._
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._


abstract class MInfo()
case class MCourse(anno: Integer, tipo: String, i: Integer, mat: Integer, label: String) extends MInfo
case class MClass(inc: Int, nod: Int, doc: Int, label: String) extends MInfo
case class AFile(url: String, id: Int, label: String) extends  MInfo

object Parser {
  val showIncRegex = """javascript:showInc\('(\d+)','(\w+)','(\d+)','(\d+)'\);*""".r
  val nextLevelRegex = """javascript:nextLevel\('(\d+)','(\d+)','(\d+)'\);*""".r
}

class Parser extends Logging {
  import Parser._

  def parseCourses(s : String) : List[MCourse] = {
    assert(s != null)
    val doc : Document = SSoup.parse(s)

    val classes = doc.select(".policorpolink").iterator.map( link => {
      val showIncRegex(anno, tipo, int , mat ) = link.attr("href")
      new MCourse(anno.toInt, tipo, int.toInt, mat.toInt, link.html)
    })
    val classesList = classes.toList
    log.info("Parsed " + classesList.length.toString + " courses")
    return classesList
  }

  def parseClasses(s : String) : List[MInfo] = {
    assert(s != null)
    val doc : Document = SSoup.parse(s)

    var classes = List[MInfo]();

    for (link <- doc.select("a").iterator) {
      val href: String = link.attr("href")
      link.select("img").remove()
      if (href.contains("nextLevel")) {
        val nextLevelRegex(inc, nod, doc) = href
        link.select("img").remove()
        classes = new MClass(inc.toInt, nod.toInt, doc.toInt, link.html) :: classes
      } else if (href.contains("download")) {
        classes = new AFile(href, 0, link.html) :: classes
      }
    }
    val classesList = classes.toList
    log.info("Parsed " + classesList.length.toString + " classes")
    classesList
  }

}
