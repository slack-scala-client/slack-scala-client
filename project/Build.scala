import sbt._
import Keys._
import com.github.retronym.SbtOneJar

object BuildSettings {
  val buildOrganization = "com.bryangilbert"
  val buildVersion      = "1.0"
  val buildScalaVersion = "2.11.6"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion
  )
}

object Resolvers {
  val typesafeRepo = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
  val sprayRepo = "spray repo" at "http://repo.spray.io"
}

object Dependencies {
  val akkaVersion = "2.3.9"
  val sprayVersion = "1.3.3"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
  val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

  val akkaStream = "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-M4"

  val scalaAsync = "org.scala-lang.modules" %% "scala-async" % "0.9.2"
  val dispatch = "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
  val playJson = "com.typesafe.play" %% "play-json" % "2.3.7"

  val scalatest = "org.scalatest" %% "scalatest" % "2.2.1" % "test"

  val akkaDependencies = Seq(akkaActor, akkaRemote, akkaCluster, akkaSlf4j, akkaTestkit, akkaStream)
  val miscDependencies = Seq(playJson, scalaAsync, dispatch)
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
      .settings ( SbtOneJar.oneJarSettings : _* )
      .settings ( exportJars := true )
      .settings ( resolvers ++= Seq(typesafeRepo) )
      .settings ( libraryDependencies ++= Dependencies.allDependencies )
      .settings ( scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint", "-Xfatal-warnings", "-feature") )

}