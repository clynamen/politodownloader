package io.clynamen.github.PolitoDownloader.Client

import org.eintr.loglady.Logging
import org.filippodeluca.ssoup.SSoup
import org.filippodeluca.ssoup.SSoup._
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._


abstract class Link()
case class CourseLink(anno: Integer, tipo: String, i: Integer, mat: Integer, label: String) extends Link
case class ClassLink(inc: Int, nod: Int, doc: Int, label: String) extends Link
case class FileLink(url: String, id: Int, label: String, format: String, Size : String) extends Link
case class VideoLink(url: String, id: Int, label: String) extends Link

trait VideoCourseLinkTrait {
  def id: Int
  def label: String
}
abstract class VideoCourseLink(id: Int, label: String) extends  VideoCourseLinkTrait
case class TypeAVideoCourseLink(id: Int, label: String) extends VideoCourseLink(id, label)
case class TypeBVideoCourseLink(id: Int, label: String) extends VideoCourseLink(id, label)
case class TypeCVideoCourseLink(id: Int, label: String) extends VideoCourseLink(id, label)

object Parser {
  val showIncRegex = """javascript:showInc\('(\d+)','(\w+)','(\d+)','(\d+)'\);*""".r
  val nextLevelRegex = """javascript:nextLevel\('(\d+)','(\d+)','(\d+)'\);*""".r
  val onLineLessonRegex = """javascript:onLineLesson\('L(\d+)','(\d+)'\);*""".r
  // javascript:showLearnPath('2014','198117');
  val showLearnPathRegex = """javascript:showLearnPath\('(\d+)','(\d+)'\);*""".r
  val dokeosLezRegex = """dokeosLez\('(\d+)'\)*""".r
  val nodRegex = """.+nod=(\d+)$""".r
  val lezRegex = """.+lez=(\d+)&.+""".r
}

class Parser extends Logging {
  import Parser._

  def parseCourses(s : String) : List[CourseLink] = {
    assert(s != null)
    val doc : Document = SSoup.parse(s)

    val classes = doc.select(".policorpolink").iterator.map( link => {
      val showIncRegex(anno, tipo, int , mat ) = link.attr("href")
      new CourseLink(anno.toInt, tipo, int.toInt, mat.toInt, link.html)
    })
    val classesList = classes.toList
    log.info("Parsed " + classesList.length.toString + " courses")
    return classesList
  }

  def parseDirContent(s : String) : List[Link] = {
    assert(s != null)
    val doc : Document = SSoup.parse(s)

    var classes = List[Link]();

    for (link <- doc.select("a").iterator) {
      val href: String = link.attr("href")
      link.select("img").remove()
      if (href.contains("nextLevel")) {
        val nextLevelRegex(inc, nod, doc) = href
        link.select("img").remove()
        classes = new ClassLink(inc.toInt, nod.toInt, doc.toInt, link.html) :: classes
      } else if (href.contains("download")) {
        val nodRegex(fileId) = href
        classes = new FileLink(href, fileId.toInt, link.html, "", "") :: classes
      }
    }
    val classesList = classes.toList
    log.info("Parsed " + classesList.length.toString + " classes")
    classesList
  }

  def parseVideoCoursePage(s : String) : List[Link] = {
    assert(s != null)
    val doc : Document = SSoup.parse(s)

    var classes = List[Link]();
    for (link <- doc.select("#lessonList li a").filter( e => !e.hasClass("argoLink") ).iterator) {
      val href: String = link.attr("href")
      val lezRegex(videoId) = href
      classes = new VideoLink(href, videoId.toInt, link.html) :: classes
    }
    val classesList = classes.toList
    log.info("Parsed " + classesList.length.toString + " classes")
    classesList
  }

  def parseVideoCoursesListPage(s : String) : List[VideoCourseLink] = {
    assert(s != null)
    val doc : Document = SSoup.parse(s)
    val links = doc.select("a").iterator
    val validators = List(typeAValidator _, typeBValidator _, typeCValidator _)
    val videoCoursesList = ValidatorUtils.
      applyValidatorsAndReturnOnlyValid[Element, VideoCourseLink](validators, links.toIterable).toList
    println(videoCoursesList.length)
    log.info("Parsed " + videoCoursesList.length + " classes")
    videoCoursesList
  }

  def typeAValidator(n : Element) : Option[TypeAVideoCourseLink] = {
    val href = n.attr("href")
    if (href.contains("onLineLesson")) {
      val onLineLessonRegex(id, _) = href
      Some(TypeAVideoCourseLink(id.toInt, n.text))
    }
    else
      None
  }

  def typeBValidator(n : Element) : Option[TypeBVideoCourseLink] = {
    val href = n.attr("href")
    if (href.contains("showLearnPath")) {
      val showLearnPathRegex(_, id) = href
      Some(TypeBVideoCourseLink(id.toInt, n.text))
    }
    else
      None
  }

  def typeCValidator(n : Element) : Option[TypeCVideoCourseLink] = {
    val onclick = n.attr("onclick")
    if (onclick != null && onclick.contains("dokeosLez")) {
      val dokeosLezRegex(id) = onclick
      Some(TypeCVideoCourseLink(id.toInt, n.text))
    }
    else
      None
  }

}

