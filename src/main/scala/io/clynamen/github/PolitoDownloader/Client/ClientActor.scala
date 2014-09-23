package io.clynamen.github.PolitoDownloader.Client

import java.net.URI

import akka.actor.{Actor, ActorRef}
import org.eintr.loglady.Logging

abstract class Req()
case class LoginReq() extends  Req
case class CourseReq(year: Int) extends Req
case class DirReq(pid: ContentId, url: URI, recursive: Boolean) extends  Req
case class FileReq(id : ContentId, url: URI, outputDir: String) extends Req

abstract  class ClientStatus(msg : String)
case class LoginOk(msg: String) extends ClientStatus(msg)
case class LoginFailed(msg: String) extends ClientStatus(msg)
case class FileDownloaded(fileId: ContentId, msg: String) extends ClientStatus(msg)

class ClientActor(val guiUpdateActor: ActorRef, userId : String, password: String) extends Actor with Logging {
  val client = new Client(userId, password)

  def sendIterable[T](actor: ActorRef, iterable: Iterable[T], recursive: Boolean) = {
    iterable.foreach(i=> actor ! (i, recursive))
  }

  def receive = {
    case LoginReq() => {
      val res : ClientStatus = if(client.tryLogin()) LoginOk ("User logged in") else LoginFailed("login failed")
      guiUpdateActor ! res
    }

    case CourseReq(year) => {
      sendIterable(guiUpdateActor, client.getCourses(year), false)
    }

    case DirReq(pid, url, recursive) => {
      sendIterable(guiUpdateActor, client.getDirContent(url, pid), recursive)
    }

    case FileReq(id, url, outputDir)=> {
      log.info(f"requesting download of $url in $outputDir ")
      // FIX-ME: override outputDir
      val downloadPath = f"./downloads/$outputDir/"
      val filename = client.downloadFile(url, downloadPath)
      guiUpdateActor ! FileDownloaded(id, f"""Downloaded $filename""")
    }

    case a @ _ => log.info("Unknown msg received, was: " + a);
  }
}


