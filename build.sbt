name := "akka-streams-stomp"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.3"

organization := "io.tvc"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.3",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
)
