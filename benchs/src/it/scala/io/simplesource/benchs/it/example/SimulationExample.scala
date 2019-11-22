package io.simplesource.benchs.it.example

import java.util.UUID

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.simplesource.benchs.gatling.protocol.SimpleSourceProtocolBuilder
import io.simplesource.data.Sequence
import io.simplesource.example.user.domain.{ UserCommand, UserKey }

import scala.concurrent.duration._

class SimulationExample extends Simulation {
  import Config._
  import scala.compat.java8.DurationConverters._

  val command: () => UserCommand.InsertUser = () => new UserCommand.InsertUser("Bob", "Dubois")

  import io.simplesource.benchs.gatling.protocol.Predef._

  val simpleSourceProtocol: SimpleSourceProtocolBuilder = simpleSource.withApp(app)

  val scn: ScenarioBuilder =
    scenario("Scenario 0") // A scenario is a chain of requests and pauses
      .exec(
        stream("Stream 1")
          .publishCommand("Command 1: publishCommand")(commandAPI, () => new UserKey(UUID.randomUUID().toString), command, Sequence.first)
      )
      .exec(
        stream("Stream 2").publishAndQueryCommand("Command 2: publishAndQueryCommand")(
          commandAPI,
          () => new UserKey(UUID.randomUUID().toString),
          command,
          Sequence.first,
          10.seconds.toJava
        )
      )

  setUp(scn.inject(rampUsers(100000) during 30.seconds).protocols(simpleSourceProtocol.build()))
}
