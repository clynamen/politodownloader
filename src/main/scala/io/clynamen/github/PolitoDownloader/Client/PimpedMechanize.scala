package io.clynamen.github.PolitoDownloader.Client

import java.net.URL
import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.util.Calendar

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.util.NameValuePair
import io.clynamen.github.PolitoDownloader.Utils.StringUtils
import org.eintr.loglady.Logging

import scala.collection.JavaConversions._

class PimpedMechanize extends Logging {

  var webClient : WebClient = null;
  reset()
  val infoEnabled = false

  def reset() = {
    webClient = new WebClient(BrowserVersion.FIREFOX_17)
    webClient.getOptions.setRedirectEnabled(true)
    webClient.getOptions.setCssEnabled(false)
  }

  def dumpCookies() =
    for (c <- webClient.getCookieManager.getCookies) println("Cookie: " + c.getName + ": " + c.getValue)


  def get(uri: String) : Page = {
    log.info("GET: " + uri)
    val res : Page = webClient.getPage(uri)
    printResponseStatus(res)
    res
  }

  def post(uri: String, params: Map[String, String] = null) : HtmlPage = {
    log.info("POST: " + uri)
    val res : HtmlPage = webClient.getPage(makePostRequest(uri, params))
    printResponseStatus(res)
    res
  }

  def makePostRequest(uri: String, params: Map[String, String] = null) = {
    val request = new WebRequest(new URL(uri))
    request.setHttpMethod(HttpMethod.POST)
    if (params != null) {request.setRequestParameters(toNameValuePairs(params))}
    request
  }

  def toNameValuePairs(map : Map[String, String]) : List[NameValuePair]  =
    map.map{ case (k,v) => new NameValuePair(k, v)}.toList

  def printResponseStatus(res : Page) = {
    val wRes = res.getWebResponse
    log.info("RESPONSE: at=" + res.getUrl + " : " + wRes.getStatusCode + " - " + wRes.getStatusMessage)
  }

  def downloadFile(uri: String, dir: String) : String = {
    log.info("DOWNLOAD FILE: " + uri)
    val res : Page = get(uri)
    printResponseStatus(res)
    var contentDisposition = res.getWebResponse.getResponseHeaderValue("Content-Disposition")

    var filename = {
      val today = Calendar.getInstance().getTime()
      val format = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss")
      format.format(today)
    }

    if(contentDisposition != null || contentDisposition.contains("filename")) {
      val filenameRegex = """.*filename="([^"]*)".*""".r
      try {
        val filenameRegex(matchedFileName) = contentDisposition
        if (!StringUtils.isNullOrEmpty(matchedFileName)) {
          log.info("Found Content-Disposition filename: " + matchedFileName)
          filename = matchedFileName
        }
      } catch {
        case e : Exception => {
          log.error("Exception during content disposition match: " + e.getMessage)
          log.info("Content-Disposition was: " + contentDisposition)
        }
      }
    }

    val path = Paths.get(dir, filename)
    if(Files.exists(path)) {
      log.error("File " + path.toAbsolutePath.toString + " already exist")
    } else {
      Files.createDirectories(Paths.get(dir, ""))
      Files.copy(res.getWebResponse.getContentAsStream, path)
      log.info("File downloaded at " + path.toAbsolutePath.toString)
    }

    filename
  }

}
