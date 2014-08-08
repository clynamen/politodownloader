name := "politodownloader"

version := "0.1.0"

scalaVersion := "2.10.2"

scalacOptions := Seq("-deprecation", "-unchecked", "-feature")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

// On windows throws an error due to missing directory. Create it manually
// TODO: Unable to build crawler from git. Using previously build jar instead
//lazy val crawler =
//  RootProject(uri("git://github.com/bplawler/crawler.git"))

// Add jfxrt jar due to missing modena.css file
unmanagedJars in Compile += Attributed.blank(file(System.getenv("JAVA_HOME") + "/jre/lib/ext/jfxrt.jar"))

// TODO: add https://gist.github.com/mucaho/8973013 to the list of contributors.

resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test" withSources() withJavadoc(),
  "org.scalacheck" %% "scalacheck" % "1.10.0" % "test" withSources() withJavadoc(),
  "org.eintr.loglady" %% "loglady" % "1.1.0",
  //  "net.sourceforge.htmlunit" % "htmlunit" % "2.15",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.13",
  "org.jsoup" % "jsoup" % "1.7.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.scalafx" % "scalafx_2.11" % "8.0.0-R4",
  "com.typesafe.akka" % "akka-actor_2.10" % "2.3.4",
  "com.typesafe" % "config" % "1.2.1",
  "io.spray" %%  "spray-json" % "1.2.6",
  "org.streum" %% "configrity-core" % "1.0.0"
)

mainClass in Compile := Some("io.clynamen.github.PolitoDownloader.Gui.MainWindow")

//lazy val root = project.in(file(".")).aggregate(crawler).dependsOn(crawler)

initialCommands := "import io.clynamen.github.PolitoDownloader._"

