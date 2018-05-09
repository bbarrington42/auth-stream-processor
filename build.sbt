name := "stream-auth-processor"

version := "0.1"

scalaVersion := "2.11.12"


libraryDependencies ++= {
  Seq(
    "com.amazonaws" % "amazon-kinesis-client" % "1.9.1",
    "ch.qos.logback" % "logback-core" % "1.2.3",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.scalaz" %% "scalaz-core" % "7.2.20",
    "org.scalaz" %% "scalaz-concurrent" % "7.2.20",
    "com.cocacola.freestyle.cda" %% "freestyle-common" % "2.0"
  )
}
