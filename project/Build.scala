import sbt._
import Keys._
import com.typesafe.sbt.SbtPgp.autoImport._
import sbtrelease._

object BuildSettings {
  val buildOrganization = "com.github.gilbertw1"
  val buildVersion      = "0.2.1"
  val buildScalaVersion = "2.12.1"
  val buildCrossScalaVersions = Seq("2.11.8", "2.12.1")

  val buildSettings = Seq (
    organization       := buildOrganization,
    version            := buildVersion,
    scalaVersion       := buildScalaVersion,
    crossScalaVersions := buildCrossScalaVersions,
    publishMavenStyle  := true,
    publishTo          := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <url>https://github.com/gilbertw1/slack-scala-client</url>
      <licenses>
        <license>
          <name>MIT</name>
          <url>https://opensource.org/licenses/MIT</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:gilbertw1/slack-scala-client.git</url>
        <connection>scm:git:git@github.com:gilbertw1/slack-scala-client.git</connection>
      </scm>
      <developers>
        <developer>
          <id>gilbertw1</id>
          <name>Bryan Gilbert</name>
          <url>http://bryangilbert.com</url>
        </developer>
      </developers>)
  )
}

object Resolvers {
  val typesafeRepo = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
}

object Dependencies {
  val akkaVersion = "2.4.14"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http-core" % "10.0.0"

  val scalaAsync = "org.scala-lang.modules" %% "scala-async" % "0.9.6"
  val playJson = "com.typesafe.play" %% "play-json" % "2.6.0-M1"

  val scalatest = "org.scalatest" %% "scalatest" % "3.0.0" % "test"

  val akkaDependencies = Seq(akkaActor, akkaHttp)
  val miscDependencies = Seq(playJson, scalaAsync)
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
