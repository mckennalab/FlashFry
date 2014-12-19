name := "CRISPREngineered"

version := "1.0"

scalaVersion := "2.11.4"

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

unmanagedBase <<= baseDirectory { base => base / "project" }

libraryDependencies += "org.apache.commons" % "commons-collections4" % "4.0"

libraryDependencies += "org.scala-lang" % "scala-pickling_2.11" % "0.9.1"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.2.0"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

libraryDependencies += "org.clapper" %% "avsl" % "1.0.2"

scalacOptions += "-target:jvm-1.7"

// set the main class for packaging the main jar
// 'run' will still auto-detect and prompt
// change Compile to Test to set it for the test jar
mainClass in (Compile, packageBin) := Some("main.scala.Main")

// set the main class for the main 'run' task
// change Compile to Test to set it for 'test:run'
mainClass in (Compile, run) := Some("main.scala.Main")

//scalaHome := Some(file("/Users/aaronmck/scala-2.10.3/"))
