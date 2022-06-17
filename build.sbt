ThisBuild / scalaVersion := "2.13.8"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings"
)

val circeVersion = "0.14.1"
val awsSdkVersion = "2.17.213"

// to resolve merge clash of 'module-info.class'
// see https://stackoverflow.com/questions/54834125/sbt-assembly-deduplicate-module-info-class
val assemblyMergeStrategyDiscardModuleInfo = assembly / assemblyMergeStrategy := {
  case PathList(ps @ _*) if ps.last == "module-info.class"  => MergeStrategy.discard
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.discard
  case PathList("mime.types")                               => MergeStrategy.filterDistinctLines
  /*
   * AWS SDK v2 includes a codegen-resources directory in each jar, with conflicting names.
   * This appears to be for generating clients from HTTP services.
   * So it's redundant in a binary artefact.
   */
  case PathList("codegen-resources", _*) => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val root = (project in file("."))
  .settings(
    name := "payment-failure-comms",
    assemblyJarName := s"${name.value}.jar",
    assemblyMergeStrategyDiscardModuleInfo,
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
        "org.scalatest" %% "scalatest" % "3.2.12" % Test,
        "software.amazon.awssdk" % "cloudwatch" % awsSdkVersion
      )
  )
  .enablePlugins(RiffRaffArtifact)
