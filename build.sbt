import com.github.retronym.SbtOneJar._

name := "politodownloader"

version := "0.1.0"

scalaVersion := "2.11.2"

oneJarSettings

scalacOptions := Seq("-deprecation", "-unchecked", "-feature")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "Maven" at "http://repo1.maven.org/maven2/"

resolvers += "spray" at "http://repo.spray.io/"

lazy val ssoup = RootProject(uri("git://github.com/clynamen/ssoup.git"))
  
exportJars in ssoup := true

lazy val root = Project(id = "politodownloader", base = file(".")).dependsOn(ssoup)

libraryDependencies ++= Seq(
  "org.eintr.loglady" % "loglady_2.10" % "1.1.0",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.13",
  "org.jsoup" % "jsoup" % "1.7.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.scalafx" % "scalafx_2.11" % "8.0.0-R4",
  "com.typesafe.akka" % "akka-actor_2.11" % "2.3.4",
  "org.scala-lang" % "scala-library" % "2.11.2",
  "org.streum" % "configrity-core_2.10" % "1.0.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2",
  "net.java.jemmy" % "JemmyFX" % "0.9.3-SNAPSHOT"
)

mainClass in Compile := Some("io.clynamen.github.PolitoDownloader.Gui.MainWindow")

mainClass in oneJar := Some("io.clynamen.github.PolitoDownloader.Gui.MainWindow")

// parallel execution breaks scalafx tests
parallelExecution in Test := false

initialCommands := "import io.clynamen.github.PolitoDownloader._"

