lazy val root = (project in file(".")).
  settings(
    name := "vkfs",
    version := "0.1.0",
    scalaVersion := "2.11.7"
  )

fork in run := true
  
resolvers += Resolver.jcenterRepo

libraryDependencies += "org.scalaj" %% "scalaj-http" % "1.1.6"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.11"

libraryDependencies += "com.github.serceman" % "jnr-fuse" % "0.1"

libraryDependencies += "com.twitter" %% "util-collection" % "6.27.0"

mainClass in Compile := Some("name.filejunkie.vkfs.Main")

oneJarSettings
