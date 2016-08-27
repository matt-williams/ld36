enablePlugins(ScalaJSPlugin)

name := "ld36"
version := "1.0"
scalaVersion := "2.11.7"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.0"
libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.0"

resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases")
libraryDependencies += "org.denigma" %%% "threejs-facade" % "0.0.74-0.1.7"
