import NativePackagerHelper._

organization := "candid-partners"
name := "auth-processor"

version := "0.1"

scalaVersion := "2.11.12"

// The default is to have a single directory at the top of the archive with the name of the artifact.
// We don't want that. The contents of 'code-deploy' and hence appspec.yml MUST be at the top level for CodeDeploy to work.
// The dirs bin, scripts, & lib will also be at the top level. appspec.yml directs the deployment.
topLevelDirectory := None
mappings in Universal ++= contentOf("code-deploy")


lazy val root = (project in file(".")).enablePlugins(JavaServerAppPackaging)


scalacOptions ++= Seq("-feature", "-deprecation")



libraryDependencies ++= {
  Seq(
    "com.amazonaws" % "amazon-kinesis-client" % "1.9.1",
    "ch.qos.logback" % "logback-core" % "1.2.3",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.scalaz" %% "scalaz-core" % "7.2.20",
    "com.typesafe" % "config" % "1.3.3",
    "com.cocacola.freestyle.cda" %% "freestyle-common" % "2.0"
  )
}


mainClass in Compile := Some("com.cda.Main")

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
