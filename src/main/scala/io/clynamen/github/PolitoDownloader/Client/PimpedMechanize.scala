package io.clynamen.github.PolitoDownloader.Client

import java.io.OutputStream
import java.net.{MalformedURLException, URL}
import java.nio.file.{StandardOpenOption, Files, Paths}
import java.text.SimpleDateFormat
import java.net.URI
import java.util.{Collections, Calendar}
import java.util.concurrent.{Executors, TimeUnit}

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.util.NameValuePair
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.cookie.Cookie
import io.clynamen.github.PolitoDownloader.Utils.{FileUtils, StringUtils, RunnableUtils}
import org.eintr.loglady.Logging
import com.ning.http.client._

import scala.collection.JavaConversions._

class PimpedMechanize extends Logging {


  var webClient : WebClient = null;
  reset()
  val infoEnabled = false
  val downloadThreadPool = Executors.newFixedThreadPool(2)
  val downloadExecutionSet = Collections.synchronizedSet(new java.util.HashSet[Any]())

  def reset() = {
    webClient = new WebClient(BrowserVersion.FIREFOX_17)
    webClient.getOptions.setRedirectEnabled(true)
    webClient.getOptions.setCssEnabled(false)
    webClient.getOptions.setJavaScriptEnabled(true)
  }

  def dumpCookies() =
    for (c <- webClient.getCookieManager.getCookies) println("Cookie: " + c.getName + ": " + c.getValue)

  def safeGet(uri: String) : Page = {
    webClient.getOptions.setThrowExceptionOnScriptError(false)
    val a = webClient.getOptions.isThrowExceptionOnFailingStatusCode
    webClient.getOptions.setThrowExceptionOnFailingStatusCode(false)
    val res = get(uri)
    webClient.getOptions.setThrowExceptionOnScriptError(true)
    webClient.getOptions.setThrowExceptionOnFailingStatusCode(a)
    res
  }

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

  def head(uri: String, params: Map[String, String] = null) : HtmlPage = {
    log.info("HEAD: " + uri)
    val res : HtmlPage = webClient.getPage(makeHeadRequest(uri, params))
    printResponseStatus(res)
    res
  }

  def makeRequest(uri: String, method: HttpMethod, params: Map[String, String] = null) = {
    val request = new WebRequest(new URL(uri))
    request.setHttpMethod(method)
    if (params != null) {request.setRequestParameters(toNameValuePairs(params))}
    request
  }

  def makeHeadRequest(uri: String, params: Map[String, String] = null) = {
    makeRequest(uri, HttpMethod.HEAD, params)
  }

  def makePostRequest(uri: String, params: Map[String, String] = null) = {
    makeRequest(uri, HttpMethod.POST, params)
  }

  def toNameValuePairs(map : Map[String, String]) : List[NameValuePair]  =
    map.map{ case (k,v) => new NameValuePair(k, v)}.toList

  def printResponseStatus(res : Page) = {
    val wRes = res.getWebResponse
    log.info("RESPONSE: at=" + res.getUrl + " : " + wRes.getStatusCode + " - " + wRes.getStatusMessage)
  }

  def getRealUrl(uri: String) = {
    val client = new AsyncHttpClient();
    val request = client.prepareGet(uri)
    importCookieFromWebClient(request, webClient)
    val location = request.execute(new AsyncHandler[String] {
      var loc = ""
      override def onThrowable(t: Throwable): Unit = {}

      override def onCompleted(): String = {
        loc
      }

      override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {STATE.ABORT}

      override def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {STATE.CONTINUE}

      override def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
        loc = headers.getHeaders.getFirstValue("Location")
        STATE.ABORT
      }
    }).get()
    println (f"\n\n\n\n\n\n Location: $uri -> \n $location \n\n\n\n\n")
    if(location != null && location.nonEmpty) {
      val redirectUri = new URI(location)
      new URI(uri).resolve(redirectUri).toString
    } else {
      uri
    }
  }

  def importCookieFromWebClient(request: AsyncHttpClient#BoundRequestBuilder, webClient: WebClient) = {
    val cookies = webClient.getCookieManager.getCookies
    for(c <- cookies) request.addCookie(new Cookie(c.getName, c.getValue, c.getValue, c.getDomain,
      c.getPath, 9999999999L, -1, c.isSecure, c.isHttpOnly))
  }

  def downloadFile2(uri: String, dir: String, listener : DownloadListener) : Boolean = {
    import AsyncHttpClientConfig.{Builder => ConfigBuilder}
    val config = (new ConfigBuilder)
    .setRequestTimeoutInMs(Int.MaxValue)
    .setConnectionTimeoutInMs(Int.MaxValue)
    .setIdleConnectionTimeoutInMs(Int.MaxValue)
    .setFollowRedirects(true)
    .setMaximumNumberOfRedirects(Int.MaxValue)
    .setMaxRequestRetry(2).build()
    val client = new AsyncHttpClient(config);
    val downloadUri = getRealUrl(uri)
    val request =  client.prepareGet(downloadUri)
    importCookieFromWebClient(request, webClient)
    downloadThreadPool.execute(RunnableUtils.funToRunnable( () => {
      val execution = request.execute(new AsyncHandler[String] {
          var filename : String = "unknown-filename"
          var outStream : OutputStream = null
          override def onThrowable(t: Throwable): Unit =
            log.error(f"Error while downloading $uri: " + t.getMessage)

          override def onCompleted(): String = {
            outStream.close()
            listener.completed()
            filename
          }

          override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
            assert(outStream != null, "output stream not created")
            outStream.write(bodyPart.getBodyPartBytes)
            listener.partDownloaded(bodyPart.length)
            STATE.CONTINUE
          }

          override def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
            STATE.CONTINUE
          }

          override def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
            filename = getFileName(downloadUri, headers)
            listener.started(filename)
            val path = Paths.get(dir, filename)
            if(Files.exists(path)) {
              log.error("File " + path.toAbsolutePath.toString + " already exist")
            } else {
              Files.createDirectories(Paths.get(dir, ""))
            }
            outStream = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE);
            STATE.CONTINUE
          }
        })
      execution.get(Long.MaxValue, TimeUnit.MINUTES)
      }
    ))
    true
  }

  def getFileName(uri: String, headers: HttpResponseHeaders) : String = {
    getFileNameFromHeaders(headers) match {
      case Some(name) => name
      case None => getNameFromUri(new URI(uri))
    }
  }

  def getNameFromUri(uri: URI) : String = {
    val path = uri.getPath
    val end = path.substring(Math.min(path.lastIndexOf('/'), path.length))
    if(!path.isEmpty) end
    else {
      val today = Calendar.getInstance().getTime()
      val format = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss")
      format.format(today)
    }
  }

  def getFileNameFromHeaders(headers: HttpResponseHeaders) : Option[String] = {
    var contentDisposition = headers.getHeaders.getFirstValue("Content-Disposition")

    println (f"\n\n Content Disposition: $contentDisposition \n\n")

    if(contentDisposition != null && contentDisposition.contains("filename")) {
      val filenameRegex = """.*filename="([^"]*)".*""".r
      try {
        val filenameRegex(matchedFileName) = contentDisposition
        if (!StringUtils.isNullOrEmpty(matchedFileName)) {
          log.info("Found Content-Disposition filename: " + matchedFileName)
          Some(matchedFileName)
        } else None
      } catch {
        case e : Exception => {
          log.error("Exception during content disposition match: " + e.getMessage)
          log.info("Content-Disposition was: " + contentDisposition)
          None
        }
      }
    } else None
  }

  def downloadFile(uri: String, dir: String, downloadedPartCallback : (Long) => Unit) : String = {
    log.info("DOWNLOAD FILE: " + uri)
    val res : Page = get(uri)
    printResponseStatus(res)
    var contentDisposition = res.getWebResponse.getResponseHeaderValue("Content-Disposition")

    var filename = {
      val today = Calendar.getInstance().getTime()
      val format = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss")
      format.format(today)
    }

    if(contentDisposition != null && contentDisposition.contains("filename")) {
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
      FileUtils.copy(res.getWebResponse.getContentAsStream, path, downloadedPartCallback)
      log.info("File downloaded at " + path.toAbsolutePath.toString)
    }

    filename
  }

}
