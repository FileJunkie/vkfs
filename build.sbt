lazy val root = (project in file(".")).
  settings(
    name := "vkfs",
    version := "1.0",
    scalaVersion := "2.11.7"
  )
  
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "1.1.6"