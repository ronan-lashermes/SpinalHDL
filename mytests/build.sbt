import Dependencies._

val spinalhdl_path = (new java.io.File("../../..")).getCanonicalPath + "/SpinalHDL"

lazy val spinalHdlIdslPlugin = ProjectRef(file(spinalhdl_path), "idslplugin")
lazy val spinalHdlSim = ProjectRef(file(spinalhdl_path), "sim")
lazy val spinalHdlCore = ProjectRef(file(spinalhdl_path), "core")
lazy val spinalHdlLib = ProjectRef(file(spinalhdl_path), "lib")
lazy val spinalVersion = "dev"

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "com.github.spinalhdl",
      scalaVersion := "2.11.12",
      version      := "2.0.0"
    )),
    scalacOptions +=  s"-Xplugin:${new File(spinalhdl_path + s"/idslplugin/target/scala-2.11/spinalhdl-idsl-plugin_2.11-$spinalVersion.jar")}",
    scalacOptions += s"-Xplugin-require:idsl-plugin",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.5",
      "org.yaml" % "snakeyaml" % "1.8"
    ),
    name := "TaggedUnion"
  )
  .dependsOn(spinalHdlIdslPlugin, spinalHdlSim,spinalHdlCore,spinalHdlLib)

// fork := true
