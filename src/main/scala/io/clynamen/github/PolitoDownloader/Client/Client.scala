package io.clynamen.github.PolitoDownloader.Client

import java.io.FileWriter

import io.clynamen.github.PolitoDownloader.Utils.FileUtils
import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.html.HtmlPage
import org.eintr.loglady.Logging
import scala.collection.immutable._
import io.clynamen.github.PolitoDownloader.Utils.StringUtils
import io.clynamen.github.PolitoDownloader.ClientUri
import scala.language.reflectiveCalls

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


  def getCourses() : Seq[PCourse] = {
    val coursesListPage : Page = mech.get(ClientUri.MList)
    val content = coursesListPage.getWebResponse.getContentAsString

    val parser = new Parser()
    val classes = parser.parseCourses(content)

    classes.map( c => c match {
      case MCourse (anno, tipo, i, mat, label) =>
        val url = f"https://didattica.polito.it/pls/portal30/sviluppo.materiale.incarichi?mat=$mat&aa=$anno&typ=$tipo"
        new PCourse(url, label, mat)
    })
  }

  def getClasses(url : String) : Seq[PSet] = {
    val classesListPage : Page = mech.get(url)
    val content = classesListPage.getWebResponse.getContentAsString
    //println(f"CLASSES CONTENT \n\n $content \n\n" )
    val parser = new Parser()
    val classes = parser.parseClasses(content)

    classes.map( c => c match {
      case MClass (inc, nod, doc, label) => {
        val url = f"https://didattica.polito.it/pls/portal30/sviluppo.materiale.next_level?inc=$inc&nod=$nod&doc=$doc"
        new PClass(url, label, nod)
      }
      case AFile (url, id, label) =>
        new PFile(url, label, id)
    })
  }

  def downloadFile(url: String, path: String) : String = {
    mech.downloadFile(ClientUri.PortalHost + url, path)
  }

}

abstract  class PSet( url: String,  label: String,  id: Int)
case class PCourse(url: String, label: String, id: Int) extends PSet(url, label, id)
case class PClass(url: String, label: String, id: Int) extends  PSet(url, label, id)
case class PFile(url: String, label: String, id: Int) extends  PSet(url, label, id)
