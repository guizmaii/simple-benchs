package io.simplesource.benchs.it

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.simplesource.api.{CommandAPI, CommandId}
import io.simplesource.benchs.gatling.protocol.SimpleSourceProtocolBuilder
import io.simplesource.data.Sequence
import io.simplesource.example.user.domain.{UserCommand, UserKey}

class BasicSimulation extends Simulation {
  import BenchConfigs._

  val key       = new UserKey("user2345")
  val firstName = "Bob"
  val lastName  = "Dubois"

  val request: CommandAPI.Request[UserKey, UserCommand] =
    new CommandAPI.Request[UserKey, UserCommand](
      CommandId.random,
      key,
      Sequence.first,
      new UserCommand.InsertUser(firstName, lastName)
    )

  import io.simplesource.benchs.gatling.protocol.Predef._

  val simpleSourceProtocol: SimpleSourceProtocolBuilder = simpleSource.withApp(app)

  val scn: ScenarioBuilder =
    scenario("Scenario Name") // A scenario is a chain of requests and pauses
      .exec(stream("request_1").publishCommand("command_1")(commandApi("client_id_1", aggregateName), request))
      .pause(5) // Note that Gatling has recorder real time pauses

  setUp(scn.inject(atOnceUsers(1)).protocols(simpleSourceProtocol.build()))
}
