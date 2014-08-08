package io.clynamen.github.PolitoDownloader.Gui

import java.nio.file.{Files, Paths}

import org.eintr.loglady.Logging
import org.streum.configrity.Configuration

object ConfManager extends Logging {
  val confFilename = "polito-downloader.conf"
//  val confDir = System.getenv("user.home")
  val confDir = "./"
  val confPath = Paths.get(confDir, confFilename)

  def isFirstInit = !Files.exists(confPath)

  def readConf = {
    if (Files.exists(confPath)) {
      log.info("Reading con file from: " + confPath.toAbsolutePath.toString)
      Configuration.load(confPath.toAbsolutePath.toString)
    }
    else {
      log.info("no conf file")
      Configuration()
    }
  }

  def storeConf(conf : Configuration) = {
    log.info("store conf file to " + confPath.toAbsolutePath.toString)
    conf.save(confPath.toAbsolutePath.toString)
  }

  def deleteConf() = {
    Configuration().save(confPath.toAbsolutePath.toString)
  }

}
