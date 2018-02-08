import sbt.ExclusionRule

name := "maiev"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= {
  val sparkV = "2.2.0"
  Seq(
    // for structured streaming
    "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkV % "provided",
    "org.apache.spark" %% "spark-sql" % sparkV % "provided",

    // typesafe
    "com.typesafe" % "config" % "1.3.2",

    // Zookeeper
    "com.twitter" %% "util-zk" % "6.27.0",

    // for s3a to work
    "com.amazonaws" % "aws-java-sdk" % "1.7.4" excludeAll(
      ExclusionRule("com.fasterxml.jackson.core", "jackson-annotations"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-core"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
    ),

    "org.apache.hadoop" % "hadoop-aws" % "2.7.3" excludeAll(
      ExclusionRule("com.amazonaws", "aws-java-sdk"),
      ExclusionRule("commons-beanutils"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-annotations"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-core"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
    ),

    // for cockroachdb
    "org.postgresql" % "postgresql" % "42.1.4",

    // for redshift
    "com.databricks" % "spark-redshift_2.11" % "3.0.0-preview1",
    "com.amazon.redshift" % "jdbc4" % "1.2.10.1009"
      from "https://s3.amazonaws.com/redshift-downloads/drivers/RedshiftJDBC42-1.2.10.1009.jar",

    // redis
    "redis.clients" % "jedis" % "2.9.0",

    // statsd
    "com.timgroup" % "java-statsd-client" % "3.0.1"
  )
}

assemblyMergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
  case "log4j.properties" => MergeStrategy.discard
  case m if m.toLowerCase.startsWith("meta-inf/services/") =>
    MergeStrategy.filterDistinctLines
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

javacOptions ++= Seq("-source",
  "1.8",
  "-target",
  "1.8",
  "-Xlint")

javaOptions ++= Seq("-Xms512M",
  "-Xmx2048M",
  "-XX:MaxPermSize=2048M",
  "-XX:+CMSClassUnloadingEnabled")

fork in Test := true

test in assembly := {}