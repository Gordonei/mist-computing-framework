enablePlugins(ScalaJSPlugin)

name := "mist-computing-framework"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.1"
libraryDependencies += "fr.hmil" %%% "roshttp" % "2.0.1"

skip in packageJSDependencies := false
jsDependencies += ProvidedJS / "sha256.js"
jsDependencies += "org.webjars" % "jquery" % "2.1.4" / "2.1.4/jquery.js"
    