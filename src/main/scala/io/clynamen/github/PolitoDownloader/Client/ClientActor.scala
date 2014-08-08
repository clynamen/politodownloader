package io.clynamen.github.PolitoDownloader.Client

import akka.actor.{Actor, ActorRef}
import org.eintr.loglady.Logging


abstract class MMaterial(name: String, id: Int, url: String)
case class MDir(name: String, pid: Int, id: Int, url: String, recursive: Boolean) extends  MMaterial (name, id, url)
case class MFile(name: String, pid: Int, id: Int, url: String, recursive: Boolean) extends  MMaterial (name, id, url)

abstract class Req()
case class LoginReq() extends  Req
case class ClassesReq() extends Req
case class DirReq(pid: Int, url: String, recursive: Boolean) extends  Req
case class FileReq(url: String, outputDir: String) extends Req

abstract  class ClientStatus(msg : String)
case class LoginOk(msg: String) extends ClientStatus(msg)
case class LoginFailed(msg: String) extends ClientStatus(msg)
case class FileDownloaded(msg: String) extends ClientStatus(msg)

class ClientActor(val guiUpdateActor: ActorRef, userId : String, password: String) extends Actor with Logging {
  val client = new Client(userId, password)

  def receive = {
    case LoginReq() => {
      val res : ClientStatus = if(client.tryLogin()) LoginOk ("User logged in") else LoginFailed("login failed")
      guiUpdateActor ! res
    }
    case ClassesReq() => {
      client.getCourses().map(p => MDir(p.label, 0, p.id, p.url, false)).foreach(v => guiUpdateActor ! v)
    }
    case DirReq(pid, url, recursive) => {
      client.getClasses(url).map(p => p match {
        case PClass(url, label, id) =>  guiUpdateActor ! MDir(label, pid, id, url, recursive)
        case PFile(url, label, id) => guiUpdateActor ! MFile(label, pid, id, url, recursive)
      })
    }
    case FileReq(url, outputDir)=> {
      log.info(f"requesting download of $url in $outputDir ")
      // FIX-ME: override outputDir
      val filename = client.downloadFile(url, "./downloads/")
      guiUpdateActor ! FileDownloaded(f"""Downloaded $filename""")
    }
    case a @ _ => log.info("Unknown msg received, was: " + a);
  }
}


