name := "spray-example"

resolvers += "spray repo" at "http://repo.spray.io"

val sprayVersion = "1.3.0"

libraryDependencies ++= Seq(
  "commons-codec"           % "commons-codec"    % "1.9",
  "com.typesafe.akka"      %% "akka-actor"       % "2.3.0",
  "io.spray"                % "spray-can"        % sprayVersion,
  "io.spray"                % "spray-routing"    % sprayVersion,
  "io.spray"               %% "spray-json"       % "1.2.5"
)

Revolver.settings

packageArchetype.java_application
