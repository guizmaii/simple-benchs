package io.simplesource.benchs.it

import io.gatling.core.Predef._
import io.simplesource.api.{CommandAPI, CommandId}
import io.simplesource.data.Sequence
import io.simplesource.example.user.domain.{UserCommand, UserKey}

class BasicSimulation extends Simulation {

  val key              = new UserKey("user2345")
  val firstName        = "Bob"
  val lastName         = "Dubois"

  val commandApi: CommandAPI[UserKey, UserCommand] = BenchConfigs.startApp()

  val request: CommandAPI.Request[UserKey, UserCommand] =
    new CommandAPI.Request[UserKey, UserCommand](
      CommandId.random,
      key,
      Sequence.first,
      new UserCommand.InsertUser(firstName, lastName)
    )

  commandApi.publishCommand(request)

}
