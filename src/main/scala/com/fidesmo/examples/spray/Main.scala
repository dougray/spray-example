package com.fidesmo.examples.spray

import akka.actor.{ ActorSystem, Props }
import akka.io.IO

import com.typesafe.config._

import spray.can.Http

object Main extends App {
  val config = ConfigFactory.load()

  implicit val system = ActorSystem("spray-example", config)

  val httpActor = IO(Http)

  val service = system.actorOf(WebServiceActor.props, "web-service")

  val serverConfig = config.getConfig("server")

  val port = serverConfig.getInt("port")
  val host = serverConfig.getString("host")

  httpActor ! Http.Bind(service, host, port = port)
}
