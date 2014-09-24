package io.clynamen.github.PolitoDownloader.Client

import java.net.URI

import akka.actor.{Actor, ActorRef}
import io.clynamen.github.PolitoDownloader.ClientUri
import org.eintr.loglady.Logging

abstract class Req()
case class LoginReq() extends  Req
case class CourseReq(year: Int) extends Req
case class VideoCourseReq(year: Int) extends Req
case class DirReq(pid: ContentId, url: URI, recursive: Boolean) extends  Req
case class VideoDirReq(pid: ContentId, linkData: VideoCourseLink, recursive: Boolean) extends  Req
case class FileReq(id : ContentId, url: URI, outputDir: String) extends Req
case class VideoReq(id : ContentId, url: URI, outputDir: String, format: VideoFileFormat.VideoFileFormat) extends Req

abstract  class ClientStatus(msg : String)
case class LoginOk(msg: String) extends ClientStatus(msg)
case class LoginFailed(msg: String) extends ClientStatus(msg)

abstract class DownloadProgress(id: ContentId)
case class PartDownloaded(id: ContentId, size: Long) extends  DownloadProgress(id)
case class DownloadCompleted(id: ContentId) extends  DownloadProgress(id)
case class DownloadStarted(id: ContentId, filename: String) extends  DownloadProgress(id)

class ClientActor(val guiUpdateActor: ActorRef, userId : String, password: String) extends Actor with Logging {
  val client = new Client(userId, password)

  def sendIterable[T](actor: ActorRef, iterable: Iterable[T], recursive: Boolean, currentTreeId: Int) = {
    iterable.foreach(i=> actor ! (i, recursive, currentTreeId))
  }

  def makeDownloadedPartCallback(id: ContentId) = (downloadedBytes: Long) => {
    guiUpdateActor ! PartDownloaded(id, downloadedBytes)
  }

  def downloadFile(id: ContentId, url: URI, outputDir: String) = {
    log.info(f"requesting download of $url in $outputDir ")
    val downloadPath = f"./downloads/$outputDir/"
    client.downloadFile(url, downloadPath, new DownloadListener {
      override def partDownloaded(size: Long): Unit = guiUpdateActor ! PartDownloaded(id, size)

      override def completed(): Unit = guiUpdateActor ! DownloadCompleted(id)

      override def started(filename: String): Unit = guiUpdateActor ! DownloadStarted(id, filename)
    })
  }

  def receive = {
    case LoginReq() => {
      val res : ClientStatus = if(client.tryLogin()) LoginOk ("User logged in") else LoginFailed("login failed")
      guiUpdateActor ! res
    }

    case (currentTreeId: Int, CourseReq(year)) => {
      sendIterable(guiUpdateActor, client.getCourses(year), false, currentTreeId)
    }

    case (currentTreeId: Int, VideoCourseReq(year)) => {
      sendIterable(guiUpdateActor, client.getVideoCourses(year), false, currentTreeId)
    }

    case (currentTreeId: Int, VideoDirReq(pid, linkData, recursive)) => {
      sendIterable(guiUpdateActor, client.getVideoDirContent(linkData, pid), recursive, currentTreeId)
    }

    case (currentTreeId: Int, DirReq(pid, url, recursive)) => {
      sendIterable(guiUpdateActor, client.getDirContent(url, pid), recursive, currentTreeId)
    }

    case FileReq(id, url, outputDir)=> {
      downloadFile(id, new URI(ClientUri.PortalHost + url), outputDir)
    }

    case VideoReq(id, url, outputDir, format)=> {
      log.info(f"requesting download of video $url")
      val downloadUrl = client.getVideoUrlForFormat(url, format)
      downloadFile(id, downloadUrl, outputDir)
    }

    case a @ _ => log.info("Unknown msg received, was: " + a);
  }
}


