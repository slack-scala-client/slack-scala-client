import sbt._
import Keys._
import com.typesafe.sbt.SbtPgp.autoImport._
import sbtrelease._

object BuildSettings {
  val buildOrganization = "com.github.slack-scala-client"
  val buildVersion      = "0.2.5"
  val buildScalaVersion = "2.12.7"
  val buildCrossScalaVersions = Seq("2.11.12", "2.12.7")

  val settings = Seq (
    organization       := buildOrganization,
    version            := buildVersion,
    scalaVersion       := buildScalaVersion,
    crossScalaVersions := buildCrossScalaVersions,
    publishMavenStyle  := true,
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
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
      <url>https://github.com/slack-scala-client/slack-scala-client</url>
      <licenses>
        <license>
          <name>MIT</name>
          <url>https://opensource.org/licenses/MIT</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:slack-scala-client/slack-scala-client.git</url>
        <connection>scm:git:git@github.com:slack-scala-client/slack-scala-client.git</connection>
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

object Dependencies {
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.5.11"
  val akkaHttp = "com.typesafe.akka" %% "akka-http-core" % "10.0.11"

  val scalaAsync = "org.scala-lang.modules" %% "scala-async" % "0.9.7"
  val playJson = "com.typesafe.play" %% "play-json" % "2.6.9"

  val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % "test"

  val jodaConvert = "org.joda" % "joda-convert" % "1.8.1" // https://stackoverflow.com/a/13856382/118587

  val akkaDependencies = Seq(akkaHttp)
  val miscDependencies = Seq(playJson, scalaAsync, jodaConvert)
  val testDependencies = Seq(scalatest)

  val allDependencies = akkaDependencies ++ miscDependencies ++ testDependencies
}
