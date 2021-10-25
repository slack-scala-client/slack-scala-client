lazy val slackScalaClientModels = 
  Project ("slack-scala-client-models", file("models"))
    .settings ( BuildSettings.settings : _* )
    .settings ( libraryDependencies ++= Seq(Dependencies.playJson) )
    .settings ( scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xfatal-warnings", "-Xlint", "-feature"))

lazy val slackScalaClient =
  Project ("slack-scala-client", file("."))
    .dependsOn(slackScalaClientModels)
    .settings ( BuildSettings.settings : _* )
      .settings ( libraryDependencies ++= Dependencies.allDependencies ++ Seq(Dependencies.scalaJava8Compat))
    .settings ( scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xfatal-warnings", "-Xlint", "-feature") )

releasePublishArtifactsAction := PgpKeys.publishSigned.key
