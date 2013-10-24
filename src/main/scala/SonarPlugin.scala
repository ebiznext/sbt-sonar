package com.ebiznext.sbt.plugins

import sbt._
import Keys._

/**
 * @author stephane.manciot@ebiznext.com
 *
 */
object SonarPlugin extends Plugin {

  private lazy val defaultSonarProperties = settingKey[Seq[(String, String)]]("Properties to be used for sonar publication")

  private lazy val sonarAntVersion = "2.1"

  trait Keys {

    lazy val Config = config("sonar") extend(Compile) hide
    lazy val sonarProperties = settingKey[Seq[(String, String)]]("Properties to be used for sonar publication")
    lazy val sonarVersion = settingKey[String]("Sonar version")
    lazy val sonarPublish = taskKey[Unit]("Publish sonar metrics")

  }  

  private object SonarDefaults extends Keys {
    val settings = Seq(
      sonarVersion := "3.5",
      sonarProperties := Seq(),
      defaultSonarProperties := Seq(
      	"sonar.projectBaseDir" -> baseDirectory.value.getAbsolutePath,
      	"sonar.verbose" -> "true",
      	"sonar.host.url" -> "http://localhost:9000",
      	"sonar.jdbc.url" -> "jdbc:h2:tcp://localhost:9092/sonar",
        "sonar.jdbc.driverClassName" -> "org.h2.Driver",
        "sonar.jdbc.username" -> "sonar",
        "sonar.jdbc.password" -> "sonar",
        "sonar.dynamicAnalysis" -> "reuseReports",
        "sonar.projectKey" -> (organization.value + ":" + name.value),
        "sonar.projectName" -> name.value,
        "sonar.projectVersion" -> version.value,
        "sonar.language" -> "java", // "scala"
        "sonar.sources" -> ((sourceDirectories in Compile).value mkString(",")), //(javaSource in Compile).value 
        "sonar.tests" -> ((sourceDirectories in Test).value mkString(",")), //(javaSource in Test).value
        "sonar.binaries" -> (Seq((classDirectory in Compile).value, (classDirectory in Test).value) mkString(",")),
        "sonar.working.directory" -> (target.value / ".sonar").getAbsolutePath
        // sonar.sourceEncoding, sonar.exclusions, sonar.skippedModules, sonar.includedModules, sonar.profile, sonar.working.directory
        // http://docs.codehaus.org/display/SONAR/Analysis+Parameters
        // http://docs.codehaus.org/display/SONAR/Project+Administration#ProjectAdministration-ExcludingFiles
        // sonar.links.homepage, sonar.links.ci, sonar.links.issue, sonar.links.scm, sonar.links.scm_dev
      ),
      resolvers += "Codehaus Maven repository" at "http://repository.codehaus.org/",
      libraryDependencies ++= SonarDependencies.dependencies(sonarVersion.value, Config)
    )
  }

  object sonar extends Keys {
  	lazy val settings = Seq(ivyConfigurations += Config) ++ SonarDefaults.settings ++ Seq(
      managedClasspath in sonarPublish <<= (classpathTypes in sonarPublish, update) map { (ct, report) =>
          Classpaths.managedJars(Config, ct, report)
      },
  	  sonarPublish := {
        val s: TaskStreams = streams.value
        val classpath : Seq[File] = ((managedClasspath in sonarPublish).value).files
	      val props = Map.empty[String, String] ++ defaultSonarProperties.value ++ sonarProperties.value ++ Seq(
	    	"sonar.libraries" -> (update.value.select( configurationFilter(name = "*") ) mkString(",")))
        for(d<-(sourceDirectories in Compile).value) IO.createDirectory(d)
        for(d<-(sourceDirectories in Test).value) IO.createDirectory(d)
        IO.createDirectory((classDirectory in Compile).value)
        IO.createDirectory((classDirectory in Test).value)
        new EmbeddedRunner(classpath, props).publish()
  	  }
    )
  }

  object SonarDependencies {
    def dependencies(sonarVersion: String, Config:Configuration) : Seq[ModuleID] = Seq[ModuleID](
    	"org.codehaus.sonar-plugins" % "sonar-ant-task" % sonarAntVersion % Config.name,
        "org.apache.ant" % "ant" % "1.8.4" % Config.name
    )
  }

class EmbeddedRunner(val classpath : Seq[File], val props : Map[String, String]) {

    //lazy val oldContextClassLoader = Thread.currentThread.getContextClassLoader

    import sbt.classpath.ClasspathUtilities

    lazy val classLoader = ClasspathUtilities.toLoader(classpath)

    lazy val projectClass = classLoader.loadClass("org.apache.tools.ant.Project")
    //lazy val sonarTaskClass = classLoader.loadClass("org.sonar.ant.SonarTask")
    lazy val embeddedRunnerClass = classLoader.loadClass("org.sonar.runner.api.EmbeddedRunner")

    lazy val createMethod = embeddedRunnerClass.getMethod("create")
    //lazy val addExtensionsMethod = embeddedRunnerClass.getMethod("addExtensions", classOf[java.util.List[java.lang.Object]])
    lazy val addPropertiesMethod = embeddedRunnerClass.getMethod("addProperties", classOf[java.util.Properties])
    lazy val setAppMethod = embeddedRunnerClass.getMethod("setApp", classOf[java.lang.String], classOf[java.lang.String])
    lazy val executeMethod = embeddedRunnerClass.getMethod("execute")

    def publish() : Unit = {
        try{
          //Thread.currentThread.setContextClassLoader(classLoader)
          val embeddedRunnerClass = createMethod.invoke(null)
          val project = projectClass.newInstance()
          //addExtensionsMethod.invoke(embeddedRunnerClass, project.asInstanceOf[AnyRef])
          var properties = new java.util.Properties()
          for((k, v) <- props) properties.setProperty(k,v)
          addPropertiesMethod.invoke(embeddedRunnerClass, properties.asInstanceOf[AnyRef])
          setAppMethod.invoke(embeddedRunnerClass, "Ant", sonarAntVersion)
          executeMethod.invoke(embeddedRunnerClass)
        }
        finally{
          //Thread.currentThread.setContextClassLoader(oldContextClassLoader)          
        }
    }
}

}