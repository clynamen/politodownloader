package io.clynamen.github.PolitoDownloader

abstract class UIMaterial(label: String, url: String);
case class UIDir(label: String, url: String) extends UIMaterial(label, url)
case class UIFile(label: String, url: String) extends UIMaterial(label, url)
