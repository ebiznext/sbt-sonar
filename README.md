sbt-sonar
=========

an sbt plugin to publish metrics within sonar

## Requirements

* [SBT 0.13+](http://www.scala-sbt.org/)


## Quick start

Add plugin to *project/plugins.sbt*:

```scala

resolvers += "Sonatype Repository" at "https://oss.sonatype.org/content/groups/public"

addSbtPlugin("com.ebiznext.sbt.plugins" % "sbt-sonar" % "0.1.1")
```

For *.sbt* build definitions, inject the plugin settings in *build.sbt*:

```scala
seq(sonar.settings :_*)
```

For *.scala* build definitions, inject the plugin settings in *Build.scala*:

```scala
Project(..., settings = Project.defaultSettings ++ com.ebiznext.sbt.plugins.SonarPlugin.sonar.settings)
```

## Configuration

Plugin keys are located in `com.ebiznext.sbt.plugins.SonarPlugin.Keys`

## Commands

```sonarPublish``` To publish metrics to sonar
