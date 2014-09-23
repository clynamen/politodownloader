package io.clynamen.github.PolitoDownloader.Utils

import java.nio.file.{StandardOpenOption, Files, Path}

import scala.io.Source
import java.net.URI
import java.net.URL
import java.io.InputStream;
import java.io.OutputStream;

object FileUtils {

  private val BUFFER_SIZE = 8192;

  def slurp(s: String) : String = {
    Source.fromFile(s).mkString
  }
  def slurp(s: URI) : String = {
    Source.fromFile(s).mkString
  }
  def slurp(s: URL) : String = {
    slurp(s.toURI)
  }

  private def nullCallback(copied: Long) = Unit

  def copy(in : InputStream, target: Path, callback: (Long) => Unit = nullCallback) : Long =
    {

      assert(in != null)

      val ostream = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE);

      // do the copy
      val out : OutputStream= ostream
      copyStream(in, out, callback);
    }

  /**
   * Reads all bytes from an input stream and writes them to an output stream.
   */
   def copyStream(source : InputStream, sink: OutputStream, callback: (Long) => Unit = nullCallback) : Long =
    {
      var nread = 0L
      val buf: Array[Byte] = new Array[Byte](BUFFER_SIZE)
      var n = 0
      var lastCallbackBytes = 0L
      val callbackChunkSize = 10*1024 // 10KiB
      while ( {n = source.read(buf); n} > 0) {
        sink.write(buf, 0, n)
        nread += n
        if(nread - lastCallbackBytes > 10*1024) {
          callback(nread)
          lastCallbackBytes = nread
        }
      }
      nread
    }

}
