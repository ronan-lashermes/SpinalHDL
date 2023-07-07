

ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.12.16"
ThisBuild / organization := "example"


val spinalhdl_path = (new java.io.File("../../..")).getCanonicalPath + "/SpinalHDL"

lazy val spinalHdlIdslPlugin = ProjectRef(file(spinalhdl_path), "idslplugin")
lazy val spinalHdlSim = ProjectRef(file(spinalhdl_path), "sim")
lazy val spinalHdlCore = ProjectRef(file(spinalhdl_path), "core")
lazy val spinalHdlLib = ProjectRef(file(spinalhdl_path), "lib")
lazy val spinalVersion = "dev"

lazy val taggedunion = (project in file("."))
  .settings(
    Compile / scalaSource := baseDirectory.value / "hw" / "spinal",
    inThisBuild(List(
      organization := "com.github.spinalhdl",
      // scalaVersion := "2.11.12",
      scalaVersion := "2.12.16",
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

fork := true

// ThisBuild / version := "1.0"
// ThisBuild / scalaVersion := "2.12.16"
// ThisBuild / organization := "org.example"

// val spinalVersion = "1.8.1"
// val spinalCore = "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion
// val spinalLib = "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion
// val spinalIdslPlugin = compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)

// lazy val taggedunion = (project in file("."))
//   .settings(
//     Compile / scalaSource := baseDirectory.value / "hw" / "spinal",
//     libraryDependencies ++= Seq(spinalCore, spinalLib, spinalIdslPlugin)
//   )

// fork := true

