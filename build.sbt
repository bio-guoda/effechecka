organization := "effechecka"

version := "0.2"

scalaVersion := "2.11.11"
ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.4.19"
  val akkaHttpV = "10.0.9"
  val scalaTestV = "3.0.1"
  val AvroVersion = "1.8.2"
  val ConfigVersion = "1.3.1"
  val H2Version = "1.4.196"
  val HadoopVersion = "2.7.4"
  val JacksonVersion = "2.9.1"
  val Log4jVersion = "2.7"
  val ParquetVersion = "1.8.3"
  val ScalatestVersion = "3.0.3"
  val Slf4jVersion = "1.7.25"
  val UnivocityVersion = "2.5.7"

  Seq(
    "org.effechecka" %% "effechecka-selector" % "0.0.3",
    "org.slf4j" % "slf4j-log4j12" % "1.7.25",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion,
    "com.univocity" % "univocity-parsers" % UnivocityVersion,
    "org.apache.hadoop" % "hadoop-common" % HadoopVersion exclude("org.slf4j", "slf4j-log4j12"),
    "org.apache.hadoop" % "hadoop-hdfs" % HadoopVersion,
    "com.h2database" % "h2" % H2Version,
    "org.apache.avro" % "avro" % AvroVersion,
    "org.apache.parquet" % "parquet-avro" % ParquetVersion,
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.typesafe" % "config" % ConfigVersion,
    "org.slf4j" % "slf4j-api" % Slf4jVersion,
    "commons-lang" % "commons-lang" % "2.6",
    "org.apache.logging.log4j" % "log4j-api" % Log4jVersion % "test",
    "org.apache.logging.log4j" % "log4j-core" % Log4jVersion % "test",
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4jVersion % "test",
    "com.google.guava" % "guava" % "22.0",
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % "test",
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaV % "test",
    "org.scalatest" %% "scalatest" % scalaTestV % "test"
  )
}

resolvers += "effechecka-releases" at "https://s3.amazonaws.com/effechecka/releases"

mainClass in Compile := Some("effechecka.WebApi")