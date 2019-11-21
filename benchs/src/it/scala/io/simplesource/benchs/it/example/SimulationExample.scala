package io.simplesource.benchs.it.example

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.simplesource.api.{CommandAPI, CommandId}
import io.simplesource.benchs.gatling.protocol.SimpleSourceProtocolBuilder
import io.simplesource.data.Sequence
import io.simplesource.example.user.domain.{UserCommand, UserKey}

import scala.concurrent.duration._

class SimulationExample extends Simulation {
  import Config._

  val key       = new UserKey("user2345")
  val firstName = "Bob"
  val lastName  = "Dubois"

  val command: UserCommand = new UserCommand.InsertUser(firstName, lastName)

  import io.simplesource.benchs.gatling.protocol.Predef._

  val simpleSourceProtocol: SimpleSourceProtocolBuilder = simpleSource.withApp(app)

  def newRequest() = new CommandAPI.Request(CommandId.random, key, Sequence.first(), command)

  val scn: ScenarioBuilder =
    scenario("Scenario 0") // A scenario is a chain of requests and pauses
      .exec(stream("Request 1").publishCommand("Command 1")(commandAPI, key, command))
      .pause(5) // Note that Gatling has recorder real time pauses
      .exec(stream("Request 2").publishCommand("Command 2")(commandAPI, key, command))
      .pause(5) // Note that Gatling has recorder real time pauses
      .exec(stream("Request 3").publishCommand("Command 3")(commandAPI, key, command))

  setUp(scn.inject(rampUsers(10000) during 2.minutes).protocols(simpleSourceProtocol.build()))
}
