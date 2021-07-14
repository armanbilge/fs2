import com.typesafe.tools.mima.core._
import sbtcrossproject.crossProject

addCommandAlias("fmt", "; compile:scalafmt; test:scalafmt; it:scalafmt; scalafmtSbt")
addCommandAlias(
  "fmtCheck",
  "; compile:scalafmtCheck; test:scalafmtCheck; it:scalafmtCheck; scalafmtSbtCheck"
)
addCommandAlias("testJVM", ";rootJVM/test")
addCommandAlias("testJS", "rootJS/test")

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / stQuiet := true

ThisBuild / baseVersion := "3.0"

ThisBuild / organization := "com.armanbilge"
ThisBuild / organizationName := "Arman Bilge"
sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"

ThisBuild / homepage := Some(url("https://github.com/armanbilge/fs2"))
ThisBuild / startYear := Some(2013)

val NewScala = "2.13.6"

ThisBuild / crossScalaVersions := Seq("3.0.1", "2.12.14", NewScala)

ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.16")

ThisBuild / spiewakCiReleaseSnapshots := true

ThisBuild / spiewakMainBranches := List("main", "series/2.5.x", "series/armanbilge")

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("compile"))
  // WorkflowStep.Sbt(List("fmtCheck", "test", "mimaReportBinaryIssues")),
  // WorkflowStep.Sbt(List("coreJVM/it:test")) // Memory leak tests fail intermittently on CI
  // WorkflowStep.Run(
  //   List("cd scalafix", "sbt testCI"),
  //   name = Some("Scalafix tests"),
  //   cond = Some(s"matrix.scala == '$NewScala'")
  // )
)

ThisBuild / scmInfo := Some(
  ScmInfo(url("https://github.com/armanbilge/fs2"), "git@github.com:armanbilge/fs2.git")
)

ThisBuild / licenses := List(("MIT", url("http://opensource.org/licenses/MIT")))

ThisBuild / testFrameworks += new TestFramework("munit.Framework")
ThisBuild / doctestTestFramework := DoctestTestFramework.ScalaCheck

ThisBuild / publishGithubUser := "mpilquist"
ThisBuild / publishFullName := "Michael Pilquist"
ThisBuild / developers ++= List(
  "pchiusano" -> "Paul Chiusano",
  "pchlupacek" -> "Pavel Chlupáček",
  "SystemFw" -> "Fabio Labella",
  "alissapajer" -> "Alissa Pajer",
  "djspiewak" -> "Daniel Spiewak",
  "fthomas" -> "Frank Thomas",
  "runarorama" -> "Rúnar Ó. Bjarnason",
  "jedws" -> "Jed Wesley-Smith",
  "durban" -> "Daniel Urban"
).map { case (username, fullName) =>
  Developer(username, fullName, s"@$username", url(s"https://github.com/$username"))
}

ThisBuild / fatalWarningsInCI := false

ThisBuild / Test / javaOptions ++= Seq(
  "-Dscala.concurrent.context.minThreads=8",
  "-Dscala.concurrent.context.numThreads=8",
  "-Dscala.concurrent.context.maxThreads=8"
)
ThisBuild / Test / run / javaOptions ++= Seq("-Xms64m", "-Xmx64m")
ThisBuild / Test / parallelExecution := false

ThisBuild / initialCommands := s"""
    import fs2._, cats.effect._, cats.effect.implicits._, cats.effect.unsafe.implicits.global, cats.syntax.all._, scala.concurrent.duration._
  """

ThisBuild / mimaBinaryIssueFilters ++= Seq(
  // No bincompat on internal package
  ProblemFilters.exclude[Problem]("fs2.internal.*"),
  // Mima reports all ScalaSignature changes as errors, despite the fact that they don't cause bincompat issues when version swapping (see https://github.com/lightbend/mima/issues/361)
  ProblemFilters.exclude[IncompatibleSignatureProblem]("*"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("fs2.Pull#MapOutput.apply"),
  ProblemFilters.exclude[IncompatibleResultTypeProblem]("fs2.Pull#MapOutput.fun"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("fs2.Pull#MapOutput.copy"),
  ProblemFilters.exclude[IncompatibleResultTypeProblem]("fs2.Pull#MapOutput.copy$default$2"),
  ProblemFilters.exclude[IncompatibleMethTypeProblem]("fs2.Pull#MapOutput.this"),
  ProblemFilters.exclude[AbstractClassProblem]("fs2.Pull$CloseScope"),
  ProblemFilters.exclude[DirectAbstractMethodProblem]("fs2.Pull#CloseScope.*"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("fs2.Pull#BindBind.this"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("fs2.Pull#CloseScope.*"),
  ProblemFilters.exclude[MissingClassProblem]("fs2.Pull$CloseScope$"),
  ProblemFilters.exclude[ReversedAbstractMethodProblem]("fs2.Pull#CloseScope.*"),
  ProblemFilters.exclude[Problem]("fs2.io.Watcher#Registration.*"),
  ProblemFilters.exclude[Problem]("fs2.io.Watcher#DefaultWatcher.*"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("fs2.io.net.tls.TLSContext.clientBuilder"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("fs2.io.net.tls.TLSContext.serverBuilder"),
  ProblemFilters.exclude[ReversedMissingMethodProblem](
    "fs2.io.net.tls.TLSContext.dtlsClientBuilder"
  ),
  ProblemFilters.exclude[ReversedMissingMethodProblem](
    "fs2.io.net.tls.TLSContext.dtlsServerBuilder"
  ),
  ProblemFilters.exclude[Problem]("fs2.io.net.tls.TLSEngine*"),
  // start #2453 cross-build fs2.io for scala.js
  // private implementation classes
  ProblemFilters.exclude[MissingClassProblem]("fs2.io.net.Socket$IntCallbackHandler"),
  ProblemFilters.exclude[MissingClassProblem]("fs2.io.net.Socket$BufferedReads"),
  ProblemFilters.exclude[MissingClassProblem]("fs2.io.net.SocketGroup$AsyncSocketGroup"),
  ProblemFilters.exclude[MissingClassProblem]("fs2.io.net.Socket$AsyncSocket"),
  ProblemFilters.exclude[MissingClassProblem](
    "fs2.io.net.DatagramSocketGroup$AsyncDatagramSocketGroup"
  ),
  ProblemFilters.exclude[MissingClassProblem]("fs2.io.net.unixsocket.UnixSockets$AsyncSocket"),
  ProblemFilters.exclude[MissingClassProblem]("fs2.io.net.unixsocket.UnixSockets$AsyncUnixSockets"),
  ProblemFilters.exclude[MissingClassProblem]("fs2.io.net.tls.TLSContext$Builder$AsyncBuilder"),
  // sealed traits
  ProblemFilters.exclude[NewMixinForwarderProblem]("fs2.io.net.Network.*"),
  ProblemFilters.exclude[NewMixinForwarderProblem]("fs2.io.net.tls.TLSContext.*"),
  ProblemFilters.exclude[InheritedNewAbstractMethodProblem]("fs2.io.net.tls.TLSContext.*")
  // end #2453
)

lazy val root = project
  .in(file("."))
  .enablePlugins(NoPublishPlugin, SonatypeCiReleasePlugin)
  .aggregate(node)

lazy val rootJVM = project
  .in(file("."))
  .enablePlugins(NoPublishPlugin)
lazy val rootJS =
  project.in(file(".")).enablePlugins(NoPublishPlugin).aggregate(node)

lazy val IntegrationTest = config("it").extend(Test)

// lazy val core = crossProject(JVMPlatform, JSPlatform)
//   .in(file("core"))
//   .configs(IntegrationTest)
//   .settings(Defaults.itSettings: _*)
//   .settings(
//     inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)
//   )
//   .settings(
//     name := "fs2-core",
//     mimaPreviousArtifacts := Set.empty,
//     sonatypeCredentialHost := "s01.oss.sonatype.org",
//     Compile / scalafmt / unmanagedSources := (Compile / scalafmt / unmanagedSources).value
//       .filterNot(_.toString.endsWith("NotGiven.scala")),
//     Test / scalafmt / unmanagedSources := (Test / scalafmt / unmanagedSources).value
//       .filterNot(_.toString.endsWith("NotGiven.scala"))
//   )
//   .settings(
//     name := "fs2-core",
//     libraryDependencies ++= Seq(
//       "org.typelevel" %%% "cats-core" % "2.6.1",
//       "org.typelevel" %%% "cats-laws" % "2.6.1" % Test,
//       "org.typelevel" %%% "cats-effect" % "3.1.1",
//       "org.typelevel" %%% "cats-effect-laws" % "3.1.1" % Test,
//       "org.typelevel" %%% "cats-effect-testkit" % "3.1.1" % Test,
//       "org.scodec" %%% "scodec-bits" % "1.1.27",
//       "org.typelevel" %%% "scalacheck-effect-munit" % "1.0.2" % Test,
//       "org.typelevel" %%% "munit-cats-effect-3" % "1.0.5" % Test,
//       "org.typelevel" %%% "discipline-munit" % "1.0.9" % Test
//     ),
//     Compile / unmanagedSourceDirectories ++= {
//       val major = if (isDotty.value) "-3" else "-2"
//       List(CrossType.Pure, CrossType.Full).flatMap(
//         _.sharedSrcDir(baseDirectory.value, "main").toList.map(f => file(f.getPath + major))
//       )
//     },
//     Test / unmanagedSourceDirectories ++= {
//       val major = if (isDotty.value) "-3" else "-2"
//       List(CrossType.Pure, CrossType.Full).flatMap(
//         _.sharedSrcDir(baseDirectory.value, "test").toList.map(f => file(f.getPath + major))
//       )
//     }
//   )

// lazy val coreJVM = core.jvm
//   .enablePlugins(SbtOsgi)
//   .settings(
//     Test / fork := true,
//     OsgiKeys.exportPackage := Seq("fs2.*"),
//     OsgiKeys.privatePackage := Seq(),
//     OsgiKeys.importPackage := {
//       val Some((major, minor)) = CrossVersion.partialVersion(scalaVersion.value)
//       Seq(s"""scala.*;version="[$major.$minor,$major.${minor + 1})"""", "*")
//     },
//     OsgiKeys.additionalHeaders := Map("-removeheaders" -> "Include-Resource,Private-Package"),
//     osgiSettings
//   )

// lazy val coreJS = core.js
//   .disablePlugins(DoctestPlugin)
//   .settings(
//     Test / scalaJSStage := FastOptStage,
//     jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
//     scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
//   )

lazy val node = project
  .in(file("node"))
  .enablePlugins(SbtOsgi, ScalablyTypedConverterGenSourcePlugin)
  .settings(
    name := "fs2-node",
    mimaPreviousArtifacts := Set.empty,
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    scalacOptions += "-nowarn",
    Compile / doc / sources := Nil,
    Compile / packageDoc / publishArtifact := false,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    Compile / npmDependencies += "@types/node" -> "16.0.0",
    useYarn := true,
    stOutputPackage := "fs2.internal.jsdeps",
    stStdlib := List("es2020"),
    OsgiKeys.exportPackage := Seq("fs2.internal.jsdeps.*"),
    OsgiKeys.privatePackage := Seq(),
    OsgiKeys.importPackage := {
      val Some((major, minor)) = CrossVersion.partialVersion(scalaVersion.value)
      Seq(
        s"""scala.*;version="[$major.$minor,$major.${minor + 1})"""",
        """fs2.*;version="${Bundle-Version}"""",
        "*"
      )
    },
    OsgiKeys.additionalHeaders := Map("-removeheaders" -> "Include-Resource,Private-Package"),
    osgiSettings
  )

// lazy val io = crossProject(JVMPlatform, JSPlatform)
//   .in(file("io"))
//   .enablePlugins(SbtOsgi)
//   .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
//   .settings(
//     name := "fs2-io",
//     mimaPreviousArtifacts := Set.empty,
//     sonatypeCredentialHost := "s01.oss.sonatype.org",
//     libraryDependencies += "com.comcast" %%% "ip4s-core" % "3.0.3",
//     OsgiKeys.exportPackage := Seq("fs2.io.*"),
//     OsgiKeys.privatePackage := Seq(),
//     OsgiKeys.importPackage := {
//       val Some((major, minor)) = CrossVersion.partialVersion(scalaVersion.value)
//       Seq(
//         s"""scala.*;version="[$major.$minor,$major.${minor + 1})"""",
//         """fs2.*;version="${Bundle-Version}"""",
//         "*"
//       )
//     },
//     OsgiKeys.additionalHeaders := Map("-removeheaders" -> "Include-Resource,Private-Package"),
//     osgiSettings
//   )
//   .jvmSettings(
//     Test / fork := true,
//     libraryDependencies += "com.github.jnr" % "jnr-unixsocket" % "0.38.8" % Optional
//   )
//   .jsSettings(
//     scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
//     Test / npmDevDependencies += "jks-js" -> "1.0.1",
//     useYarn := true
//   )
//   .dependsOn(core % "compile->compile;test->test")
//   .jsConfigure(_.dependsOn(node))

// lazy val reactiveStreams = project
//   .in(file("reactive-streams"))
//   .enablePlugins(SbtOsgi)
//   .settings(
//     name := "fs2-reactive-streams",
//     mimaPreviousArtifacts := Set.empty,
//     sonatypeCredentialHost := "s01.oss.sonatype.org",
//     Test / fork := true,
//     libraryDependencies ++= Seq(
//       "org.reactivestreams" % "reactive-streams" % "1.0.3",
//       "org.reactivestreams" % "reactive-streams-tck" % "1.0.3" % "test",
//       ("org.scalatestplus" %% "testng-6-7" % "3.2.9.0" % "test").cross(CrossVersion.for3Use2_13)
//     ),
//     OsgiKeys.exportPackage := Seq("fs2.interop.reactivestreams.*"),
//     OsgiKeys.privatePackage := Seq(),
//     OsgiKeys.importPackage := {
//       val Some((major, minor)) = CrossVersion.partialVersion(scalaVersion.value)
//       Seq(
//         s"""scala.*;version="[$major.$minor,$major.${minor + 1})"""",
//         """fs2.*;version="${Bundle-Version}"""",
//         "*"
//       )
//     },
//     OsgiKeys.additionalHeaders := Map("-removeheaders" -> "Include-Resource,Private-Package"),
//     osgiSettings
//   )
//   .dependsOn(coreJVM % "compile->compile;test->test")

// lazy val benchmark = project
//   .in(file("benchmark"))
//   .enablePlugins(JmhPlugin, NoPublishPlugin)
//   .settings(
//     name := "fs2-benchmark",
//     Test / run / javaOptions := (Test / run / javaOptions).value
//       .filterNot(o => o.startsWith("-Xmx") || o.startsWith("-Xms")) ++ Seq("-Xms256m", "-Xmx256m")
//   )
//   .dependsOn(io.jvm)

// lazy val microsite = project
//   .in(file("mdoc"))
//   .settings(
//     mdocIn := file("site"),
//     mdocOut := file("target/website"),
//     mdocVariables := Map(
//       "version" -> version.value,
//       "scalaVersions" -> crossScalaVersions.value
//         .map(v => s"- **$v**")
//         .mkString("\n")
//     ),
//     githubWorkflowArtifactUpload := false,
//     fatalWarningsInCI := false
//   )
//   .dependsOn(coreJVM, io.jvm, reactiveStreams)
//   .enablePlugins(MdocPlugin, NoPublishPlugin)

// ThisBuild / githubWorkflowBuildPostamble ++= List(
//   WorkflowStep.Sbt(
//     List("microsite/mdoc"),
//     cond = Some(s"matrix.scala == '2.13.6'")
//   )
// )

// ThisBuild / githubWorkflowAddedJobs += WorkflowJob(
//   id = "site",
//   name = "Deploy site",
//   needs = List("publish"),
//   javas = (ThisBuild / githubWorkflowJavaVersions).value.toList,
//   scalas = (ThisBuild / scalaVersion).value :: Nil,
//   cond = """
//   | always() &&
//   | needs.build.result == 'success' &&
//   | (needs.publish.result == 'success' && github.ref == 'refs/heads/main')
//   """.stripMargin.trim.linesIterator.mkString.some,
//   steps = githubWorkflowGeneratedDownloadSteps.value.toList :+
//     WorkflowStep.Use(
//       UseRef.Public("peaceiris", "actions-gh-pages", "v3"),
//       name = Some(s"Deploy site"),
//       params = Map(
//         "publish_dir" -> "./target/website",
//         "github_token" -> "${{ secrets.GITHUB_TOKEN }}"
//       )
//     )
// )
