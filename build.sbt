organization := "effechecka"

version := "0.1"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.4.4"
  val cassandraDriverV = "2.1.6"
  val scalaTestV = "2.2.5"
  Seq(
    "joda-time" % "joda-time" % "2.9.3",
    "com.sun.jersey" % "jersey-client" % "1.9",
    "org.locationtech.spatial4j" % "spatial4j" % "0.6",
    "com.vividsolutions" % "jts-core" % "1.14.0",
    "com.fasterxml.uuid" %  "java-uuid-generator" % "3.1.4",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http-core" % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-xml-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test",
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaV % "test",
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.11-M2" excludeAll (
      ExclusionRule("org.typesafe.akka")
      ),
    "com.datastax.cassandra" % "cassandra-driver-core" % cassandraDriverV,
    "org.scalatest" %% "scalatest" % scalaTestV % "test"
  )
}

