import sbt.Tests.{ Group, SubProcess }

name := "scalascriptengine"

organization := "com.axiomsl.scalascriptengine"

version := "1.3.12"

pomIncludeRepository := { _ => false }

licenses := Seq("Apache License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))

homepage := Some(url("https://github.com/axiomsl/scalascriptengine"))

scmInfo := Some(
	ScmInfo(
		url("https://github.com/axiomsl/scalascriptengine"),
		"scm:https://github.com/axiomsl/scalascriptengine.git"
	)
)

developers := List(
	Developer(
		id = "kostas.kougios@googlemail.com",
		name = "Konstantinos Kougios",
		email = "kostas.kougios@googlemail.com",
		url = url("https://github.com/kostaskougios")
	),
	Developer(
		id = "izek.greenfield@adenza.com",
		name = "Izek Greenfield",
		email = "izek.greenfield@adenza.com",
		url = url("https://github.com/igreenfield")
	)
)

publishMavenStyle := true

updateOptions := updateOptions.value.withGigahorse(false)

publishTo := {
	val nexus = "https://###maven_host_name###/"
//	if (isSnapshot.value)
	Some("release_candidates" at nexus + "content/repositories/release_candidates")
//	else
//		Some("releases" at nexus + "content/repositories/releases")
}

credentials ++= (for {
	user <- Option(System.getenv().get("MAVEN_USER"))
	pw <- Option(System.getenv().get("MAVEN_PASSWORD"))
} yield Credentials("Sonatype Nexus Repository Manager", "###maven_host_name###", user, pw)).toSeq

scalaVersion := "2.12.17"

libraryDependencies ++= Seq(
	"org.slf4j" % "slf4j-api" % "1.7.30",
	"org.scala-lang" % "scala-compiler" % "2.12.17",
	"org.scala-lang" % "scala-reflect" % "2.12.17",
	"joda-time" % "joda-time" % "2.12.2",
	"ch.qos.logback" % "logback-classic" % "1.4.5"  % Test,
	"org.scalatest" %% "scalatest" % "3.2.14" % Test,
	"commons-io" % "commons-io" % "2.11.0" % Test,
)

// fork in test cause there are conflicts with sbt classpath
def forkedJvmPerTest(testDefs: Seq[TestDefinition]) = testDefs.groupBy(
	test => test.name match {
		case "com.googlecode.scalascriptengine.SandboxSuite" =>
			test.name
		case _ => "global"
	}
).map { case (name, tests) =>
	Group(
		name = name,
		tests = tests,
		runPolicy = SubProcess(ForkOptions())
	)
}.toSeq

//definedTests in Test returns all of the tests (that are by default under src/test/scala).
Test / testGrouping := { (Test / definedTests).map(forkedJvmPerTest) }.value

Test / testOptions += Tests.Argument("-oF")
