import sbt._
import Keys._
import com.typesafe.sbt.SbtPgp.autoImport._
import sbtrelease._

object BuildSettings {
  val buildOrganization = "com.github.gilbertw1"
  val buildVersion      = "0.1.3"
  val buildScalaVersion = "2.11.7"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion
  )
}

object Resolvers {
  val typesafeRepo = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
}

object Dependencies {
  val akkaVersion = "2.3.12"
  val sprayVersion = "1.3.3"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

  val scalaAsync = "org.scala-lang.modules" %% "scala-async" % "0.9.2"
  val dispatch = "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
  val playJson = "com.typesafe.play" %% "play-json" % "2.3.9"
  val sprayWebsocket = "com.wandoulabs.akka" %% "spray-websocket" % "0.1.4"

  val scalatest = "org.scalatest" %% "scalatest" % "2.2.1" % "test"

  val akkaDependencies = Seq(akkaActor, akkaSlf4j)
  val miscDependencies = Seq(playJson, scalaAsync, dispatch, sprayWebsocket)
  val testDependencies = Seq(scalatest)

  val allDependencies = akkaDependencies ++ miscDependencies ++ testDependencies
}

object SlackScalaClient extends Build {
  import Resolvers._
  import BuildSettings._
  import Defaults._

  lazy val slackScalaClient =
    Project ("slack-scala-client", file("."))
      .settings ( buildSettings : _* )
      .settings ( resolvers ++= Seq(typesafeRepo) )
      .settings ( libraryDependencies ++= Dependencies.allDependencies )
      .settings ( scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint", "-Xfatal-warnings", "-feature") )

}