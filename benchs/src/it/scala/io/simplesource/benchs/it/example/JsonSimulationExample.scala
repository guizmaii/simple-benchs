package io.simplesource.benchs.it.example

import java.util.UUID

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.simplesource.benchs.gatling.protocol.SimpleSourceProtocolBuilder
import io.simplesource.data.Sequence
import io.simplesource.example.user.domain.{UserCommand, UserKey}

import scala.concurrent.duration._

class JsonSimulationExample extends Simulation {
  import Config._

  val newRandomUserKey: () => UserKey               = () => new UserKey(UUID.randomUUID().toString)
  val constantCommand: () => UserCommand.InsertUser = () => new UserCommand.InsertUser("Bob", "Dubois")

  import io.simplesource.benchs.gatling.protocol.Predef._

  val (app, commandAPI) = jsonAppAndClient

  val simpleSourceProtocol: SimpleSourceProtocolBuilder = simpleSource.withApp(app)

  val scn: ScenarioBuilder =
    scenario("JSON Scenario 0") // A scenario is a chain of requests and pauses
      .exec(
        stream("JSON Stream 1")
          .publishCommand("JSON Command 1: publishCommand")(commandAPI, newRandomUserKey, constantCommand, Sequence.first)
      )
      .exec {
        import scala.compat.java8.DurationConverters._

        stream("JSON Stream 2").publishAndQueryCommand("JSON Command 2: publishAndQueryCommand")(
          commandAPI,
          newRandomUserKey,
          constantCommand,
          Sequence.first,
          20.seconds.toJava
        )
      }

  setUp(scn.inject(rampUsers(100000) during 10.minutes).protocols(simpleSourceProtocol.build()))
}
