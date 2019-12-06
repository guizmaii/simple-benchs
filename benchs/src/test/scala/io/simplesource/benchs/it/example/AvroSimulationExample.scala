package io.simplesource.benchs.it.example

import java.util.UUID

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.simplesource.benchs.gatling.protocol.SimpleSourceProtocolBuilder
import io.simplesource.data.Sequence
import io.simplesource.example.user.domain.{ UserCommand, UserKey }

import scala.concurrent.duration._

class AvroSimulationExample extends Simulation {
  import Config._

  val newRandomUserKey: () => UserKey               = () => new UserKey(UUID.randomUUID().toString)
  val constantCommand: () => UserCommand.InsertUser = () => new UserCommand.InsertUser("Bob", "Dubois")

  import io.simplesource.benchs.gatling.protocol.Predef._

  val (app, commandAPI) = avroAppAndClient

  val simpleSourceProtocol: SimpleSourceProtocolBuilder = simpleSource.withApp(app)

  val scn0: ScenarioBuilder =
    scenario("Avro Scenario - PublishCommand") // A scenario is a chain of requests and pauses
      .exec(
        stream("publishCommand Stream")
          .publishCommand("Avro Command 1: publishCommand")(commandAPI, newRandomUserKey, constantCommand, Sequence.first)
      ).pause(1.minute)

  val scn1 =
    scenario("Avro Scenario - publishAndQueryCommand") // A scenario is a chain of requests and pauses
      .exec {
        import scala.compat.java8.DurationConverters._

        stream("publishAndQueryCommand Stream").publishAndQueryCommand("Avro Command 2: publishAndQueryCommand")(
          commandAPI,
          newRandomUserKey,
          constantCommand,
          Sequence.first,
          20.seconds.toJava
        )
      }.pause(1.minute)

  setUp(
    scn1.inject(rampConcurrentUsers( 0) to 1000 during 30.minutes),
  ).protocols(simpleSourceProtocol.build())
}

