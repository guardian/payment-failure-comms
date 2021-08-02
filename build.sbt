ThisBuild / scalaVersion := "3.0.1"

val circeVersion = "0.14.1"

lazy val root = (project in file("."))
  .settings(
    name := "payment-failure-comms",
    libraryDependencies ++=
      Seq(
        "org.scalatest" %% "scalatest" % "3.2.9" % Test,
        "io.circe" %% "circe-generic" % circeVersion,
        "io.circe" %% "circe-parser" % circeVersion,
        "com.squareup.okhttp3" % "okhttp" % "4.9.1"
      )
  )
