lazy val slackScalaClient =
  Project ("slack-scala-client", file("."))
    .settings ( BuildSettings.settings : _* )
    .settings ( TaglessSettings.settings : _* )
    .settings ( libraryDependencies ++= Dependencies.allDependencies )
    .settings ( scalacOptions ++= Seq("-language:higherKinds", "-unchecked", "-deprecation", "-Xfatal-warnings", "-Xlint", "-feature") )
    .settings (
      Compile / scalacOptions ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, n)) if n < 13 => "-Ypartial-unification" :: Nil
          case _ => Nil
        }
      }
    )
releasePublishArtifactsAction := PgpKeys.publishSigned.key
