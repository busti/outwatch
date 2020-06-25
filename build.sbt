import Options._


inThisBuild(Seq(
  version := "0.11.1-SNAPSHOT",

  organization := "io.github.outwatch",

  scalaVersion := crossScalaVersions.value.last,

  crossScalaVersions := Seq("2.12.10", "2.13.1"),

  licenses += ("Apache 2", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),

  homepage := Some(url("https://outwatch.github.io/")),

  scmInfo := Some(ScmInfo(
    url("https://github.com/OutWatch/outwatch"),
    "scm:git:git@github.com:OutWatch/outwatch.git",
    Some("scm:git:git@github.com:OutWatch/outwatch.git"))
  )
))

val jsdomVersion = "13.2.0"

lazy val commonSettings = Seq(
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
  addCompilerPlugin("com.github.ghik" % "silencer-plugin" % "1.6.0" cross CrossVersion.full),

  useYarn := true,

  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.2.0" % Test,
    "com.github.ghik" % "silencer-lib" % "1.6.0" % Provided cross CrossVersion.full,
  ),

  scalacOptions ++= CrossVersion.partialVersion(scalaVersion.value).map(v =>
    allOptionsForVersion(s"${v._1}.${v._2}", true)
  ).getOrElse(Nil),
  scalacOptions in (Compile, console) ~= (_.diff(badConsoleFlags))
)

lazy val librarySettings = commonSettings ++ Seq(

  scalacOptions += {
    val local = baseDirectory.value.toURI
    val remote = s"https://raw.githubusercontent.com/OutWatch/outwatch/${git.gitHeadCommit.value.get}/"
    s"-P:scalajs:mapSourceURI:$local->$remote"
  },

  publishMavenStyle := true,

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
    else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },

  pomExtra :=
    <developers>
        <developer>
        <id>jk</id>
        <name>Johannes Karoff</name>
        <url>https://github.com/cornerman</url>
        </developer>
        <developer>
        <id>ltj</id>
        <name>Luka Jacobowitz</name>
        <url>https://github.com/LukaJCB</url>
        </developer>
    </developers>,

  pomIncludeRepository := { _ => false }
)

lazy val outwatchReactive = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .in(file("reactive"))
  .settings(librarySettings)
  .settings(
    name := "OutWatch-Reactive",
    normalizedName := "outwatch-reactive",

    resolvers ++=
      ("jitpack" at "https://jitpack.io") ::
      Nil,

    libraryDependencies ++= Seq(
      "com.github.busti.colibri" %%% "colibri" % "33f30e3",
    )
  )

lazy val outwatchUtil = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .in(file("util"))
  .settings(librarySettings)
  .settings(
    name := "OutWatch-Util",
    normalizedName := "outwatch-util",
  )

lazy val outwatchMonix = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .in(file("monix"))
  .settings(librarySettings)
  .settings(
    name := "OutWatch-Monix",
    normalizedName := "outwatch-monix",

    libraryDependencies ++= Seq(
      "com.github.busti.colibri" %%% "colibri-monix" % "33f30e3",
      "io.monix"      %%% "monix"       % "3.2.2",
    )
  )

lazy val outwatchRepairDom = project
  .in(file("repairdom"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .settings(librarySettings)
  .settings(
    name := "OutWatch-RepairDom",
    normalizedName := "outwatch-repairdom",
  )

lazy val outwatch = project
  .in(file("outwatch"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatchReactive)
  .settings(librarySettings)
  .settings(
    name := "OutWatch",
    normalizedName := "outwatch",

    resolvers ++=
      ("jitpack" at "https://jitpack.io") ::
      Nil,

    libraryDependencies ++= Seq(
      "com.raquo"     %%% "domtypes" % "0.9.7",
    ),

    npmDependencies in Compile ++= Seq(
      "snabbdom" -> "git://github.com/outwatch/snabbdom.git#semver:0.7.5"
    )
  )

lazy val tests = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatchMonix, outwatchUtil, outwatchRepairDom)
  .settings(commonSettings)
  .settings(
    skip in publish := true,

    requireJsDomEnv in Test := true,
    version in installJsdom := jsdomVersion,
  )

lazy val bench = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatchMonix)
  .settings(
    skip in publish := true,

    resolvers ++=
      ("jitpack" at "https://jitpack.io") ::
      Nil,

    libraryDependencies ++=
      "com.github.fdietze.bench" %%% "bench" % "5ffab44" ::
      Nil,

    scalaJSStage in Compile := FullOptStage,
    scalacOptions ++= Seq ("-Xdisable-assertions"),
    /* scalaJSUseMainModuleInitializer := true, */

    useYarn := true,

    npmDependencies in Compile ++= Seq(
      "jsdom" -> jsdomVersion
    ),
  )


lazy val root = project
  .in(file("."))
  .settings(
    name := "outwatch-root",
    skip in publish := true,
  )
  .aggregate(outwatch, outwatchMonix, outwatchReactive, outwatchUtil, outwatchRepairDom, tests)
