lazy val slackScalaClient =
  Project ("slack-scala-client", file("."))
    .settings ( BuildSettings.settings : _* )
    .settings ( libraryDependencies ++= Dependencies.allDependencies )
    .settings ( scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xfatal-warnings", "-Xlint", "-feature") )

releasePublishArtifactsAction := PgpKeys.publishSigned.key

scapegoatVersion in ThisBuild := "1.4.1"
