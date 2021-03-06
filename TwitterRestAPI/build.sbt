name := "twitterrestapi"
 
version := "1.0"

scalaVersion := "2.11.2" 

resolvers += "spray repo" at "http://repo.spray.io"

val sprayVersion = "1.3.2"

libraryDependencies ++= Seq(
"com.typesafe.akka" %% "akka-actor" % "2.3.5",
"com.typesafe.akka" %% "akka-http-experimental" % "0.7",
"io.spray" %% "spray-routing" % sprayVersion,
"io.spray" %% "spray-client" % sprayVersion,
"io.spray" %% "spray-testkit" % sprayVersion % "test",
"org.json4s" %% "json4s-native" % "3.2.10"
)
 
