ThisBuild / scalaVersion := "2.13.8"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings"
)

val circeVersion = "0.14.1"

lazy val root = (project in file("."))
  .settings(
    name := "payment-failure-comms",
    assemblyJarName := s"${name.value}.jar",
    riffRaffAwsRegion := "eu-west-1",
    riffRaffPackageType := assembly.value,
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestProjectName := s"Retention::${name.value}",
    riffRaffArtifactResources += (file("cfn.yaml"), "cfn/cfn.yaml"),
    libraryDependencies ++=
      Seq(
        "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
        "io.circe" %% "circe-generic" % circeVersion,
        "io.circe" %% "circe-parser" % circeVersion,
        "com.squareup.okhttp3" % "okhttp" % "4.9.3",
        "dev.zio" %% "zio" % "2.0.0-RC2",
                "org.scalatest" %% "scalatest" % "3.2.11" % Test
      )
  )
  .enablePlugins(RiffRaffArtifact)
