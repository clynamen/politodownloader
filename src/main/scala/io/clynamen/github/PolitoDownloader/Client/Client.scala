package io.clynamen.github.PolitoDownloader.Client

import java.io.FileWriter


import io.clynamen.github.PolitoDownloader.Utils.FileUtils
import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.html.HtmlPage
import org.eintr.loglady.Logging
import scala.collection.immutable._
import io.clynamen.github.PolitoDownloader.Utils.StringUtils
import io.clynamen.github.PolitoDownloader.{Gui, ClientUri}
import java.net.URI
import scala.language.reflectiveCalls
import com.github.pathikrit.dijon.{parse => jsonParse}

class Client(userId : String, password: String) extends Logging {
  require(!StringUtils.isNullOrEmpty((userId)), "user id cannot be empty")
  require(!StringUtils.isNullOrEmpty((userId)), "password cannot be empty")

  val mech = new PimpedMechanize()
  val parser = new Parser()

  def reset() = mech.reset()

  // User photo can only be private
  def isUserLoggedIn() : Boolean =  {
      val res : Page = mech.get(ClientUri.ProfilePhotoUri + userId.replace("s",""))
      val contentType : String = res.getWebResponse.getContentType
      contentType.contains("image")
  }

  def statusCodeOfResult(res : Page) = res.getWebResponse.getStatusCode

  def tryLogin() : Boolean = {
    // Do get on shib page before posting login info in order
    // to get shib cookies
    log.info("Getting shib cookies")
    val shibResult : Page = mech.get(ClientUri.ShibLogin)
    //printReqResultToFile(shibResult, "shib")
    log.info("done GET shib cookies: " + statusCodeOfResult(shibResult))

    log.info("doing POST")
    val args = Map("j_username" -> userId, "j_password" -> password)
    val doc : HtmlPage = mech.post(ClientUri.UserPasswordLogin, args)
    //printReqResultToFile(doc, "post")
    log.info("done login POST: " + statusCodeOfResult(shibResult))

    //mech.dumpCookies
    isUserLoggedIn
  }

  def printReqResultToFile(res : Page, filename : String) = {
      val content = res.getWebResponse.getContentAsString
      writeToFile(filename, content)
  }

  def using[A <: {def close(): Unit}, B](param: A)(f: A => B): B =
    try { f(param) } finally { param.close() }

  def writeToFile(fileName:String, data:String) =
    using (new FileWriter(fileName)) {
      fileWriter => fileWriter.write(data)
  }

  def getTestFileContent(filename: String) = FileUtils.slurp(getClass.getResource(filename))


  def getCourses(year: Integer) : Iterable[DirectoryInfo] = {
    val coursesListPage : Page = mech.get(ClientUri.courses(year))
    val content = coursesListPage.getWebResponse.getContentAsString

    val parser = new Parser()
    val classes = parser.parseCourses(content)

    classes.map( c => c match {
      case CourseLink (anno, tipo, i, mat, label) =>
        val url = f"https://didattica.polito.it/pls/portal30/sviluppo.materiale.incarichi?mat=$mat&aa=$anno&typ=$tipo"
        new DirectoryInfo(new URI(url), label, new ContentId(ContentType.Course, mat), None)
    })
  }

  def getVideoCourses(year: Integer) : Iterable[VideoDirectoryInfo] = {
    val coursesListPage : Page = mech.get(ClientUri.videoCourses(year))
    val content = coursesListPage.getWebResponse.getContentAsString

    val parser = new Parser()
    val classes = parser.parseVideoCoursesListPage(content)

    classes.map( c => VideoDirectoryInfo(c, c.label,
                        new ContentId(ContentType.VideoDirectory, c.id), None)
    )
  }

  def addCorrectRootForVideoPageUrl(url: String) : URI = {
    if(url.startsWith("template_video.php"))
      new URI("http://elearning.polito.it/gadgets/video/"+ url)
    else if (url.startsWith("/pls/"))
      new URI("https://didattica.polito.it" + url)
    else if (url.startsWith("lp_controller"))
      new URI("http://elearning.polito.it/main/newscorm/" + url)
    else throw new Exception("Unknown url: " + url)
  }

  def getVideoDirContent(linkData : VideoCourseLink, pid : ContentId) : Iterable[VideoFileInfo] = {
    val content = getVideoCoursePage(linkData)

    val parser = new Parser()
    val classes = parser.parseVideoCoursePage(content)

    classes.map( c => {
        val contentUri = addCorrectRootForVideoPageUrl(c.url)
        new VideoFileInfo(c.label, new ContentId(ContentType.Video, c.id),
                      Some(pid), Formats(FileFormat.MP4), contentUri)
      }
    )
  }

  def getVideoCoursePage(linkData: VideoCourseLink) = {
    linkData match {
      case a : TypeAVideoCourseLink => getTypeAVideoCoursePage(a)
      case b : TypeBVideoCourseLink => getTypeBVideoCoursePage(b)
      case c : TypeCVideoCourseLink => getTypeCVideoCoursePage(c)
    }
  }

  def getTypeAVideoCoursePage(linkData: TypeAVideoCourseLink) = {
    val videoListPage : Page = mech.safeGet(ClientUri.typeAVideoList(linkData.id))
    videoListPage.getWebResponse.getContentAsString
  }

  def getTypeBVideoCoursePage(linkData: TypeBVideoCourseLink) = {
    val videoListPage : Page = mech.safeGet(ClientUri.typeBVideoList(linkData.id))
    videoListPage.getWebResponse.getContentAsString
  }

  def getTypeCVideoCoursePage(linkData: TypeCVideoCourseLink) = {
    val formDataPage = mech.get(ClientUri.typeCVideoListAccess(linkData.id))
    val formData = jsonParse(formDataPage.getWebResponse.getContentAsString)
    println(formData.toString)
    println(formData.utente.toString.replaceAll("\"", ""))
    val videoListPage : Page = mech.safeGet(ClientUri.typeCVideoList(formData))
    videoListPage.getWebResponse.getContentAsString
  }

  def getDirContent(url : URI, pid : ContentId) : Iterable[ContentInfo] = {
    val classesListPage : Page = mech.get(url.toString)
    val content = classesListPage.getWebResponse.getContentAsString
    val parser = new Parser()
    val classes = parser.parseDirContent(content)

    classes.map( c => c match {
      case ClassLink (inc, nod, doc, label) => {
        val url = f"https://didattica.polito.it/pls/portal30/sviluppo.materiale.next_level?inc=$inc&nod=$nod&doc=$doc"
        new DirectoryInfo(new URI(url), label,
          new ContentId(ContentType.Directory, nod), Some(pid))
      }
      case FileLink (url, id, label, format, size) =>
        new FileInfo(new URI(url), label,
          new ContentId(ContentType.File, id),
          Some(pid), Formats(FileFormat.DOC))
    })
  }

  def getVideoUrlForFormat(url: URI, format: VideoFileFormat.VideoFileFormat) = {
    val videoPage : Page = mech.safeGet(url.toString)
    val content = videoPage.getWebResponse.getContentAsString
    val availableLinks = new Parser().parseVideoFormatsLink(content)
    val videoLink = availableLinks(format.id)
    url.resolve(videoLink)
  }

  def downloadFile(url: URI, path: String, listener: DownloadListener) : Boolean = {
    mech.downloadFile2(url.toString, path, listener)
  }

}
