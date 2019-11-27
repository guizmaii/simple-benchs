package io.simplesource.benchs.gatling.protocol

import java.time.Duration

import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.structure.ScenarioContext
import io.simplesource.api.CommandAPI.Request
import io.simplesource.api.{ CommandAPI, CommandError, CommandId }
import io.simplesource.data.{ FutureResult, Sequence }

final class Stream(actionName: String) {
  def publishCommand[K, C](
    requestName: Expression[String]
  )(commandAPI: CommandAPI[K, C], key: () => K, command: () => C, sequence: () => Sequence): ActionBuilder =
    (ctx: ScenarioContext, next: Action) =>
      new SimpleSourceAction[K, C, Request[K, C], CommandId](actionName, requestName, ctx, next) {
        override def sendRequest(
          requestName: String,
          session: Session,
          request: CommandAPI.Request[K, C]
        ): FutureResult[CommandError, CommandId] = commandAPI.publishCommand(request)

        override def requestParamsGen(): Request[K, C] =
          new CommandAPI.Request[K, C](CommandId.random, key(), sequence(), command())
      }

  def queryCommandResult[K, C](
    requestName: Expression[String]
  )(commandAPI: CommandAPI[K, C], commandId: CommandId, timeout: Duration): ActionBuilder =
    (ctx: ScenarioContext, next: Action) =>
      new SimpleSourceAction[K, C, (CommandId, Duration), Sequence](actionName, requestName, ctx, next) {
        private final val tupled: ((CommandId, Duration)) => FutureResult[CommandError, Sequence] = (commandAPI.queryCommandResult _).tupled

        override def sendRequest(
          requestName: String,
          session: Session,
          request: (CommandId, Duration)
        ): FutureResult[CommandError, Sequence] = tupled(request)

        override def requestParamsGen(): (CommandId, Duration) = (commandId, timeout)
      }

  def publishAndQueryCommand[K, C](
    requestName: Expression[String]
  )(commandAPI: CommandAPI[K, C], key: () => K, command: () => C, sequence: () => Sequence, timeout: Duration): ActionBuilder =
    (ctx: ScenarioContext, next: Action) =>
      new SimpleSourceAction[K, C, Request[K, C], Sequence](actionName, requestName, ctx, next) {
        override def sendRequest(
          requestName: String,
          session: Session,
          request: CommandAPI.Request[K, C]
        ): FutureResult[CommandError, Sequence] = commandAPI.publishAndQueryCommand(request, timeout)

        override def requestParamsGen(): CommandAPI.Request[K, C] =
          new CommandAPI.Request[K, C](CommandId.random, key(), sequence(), command())
      }
}
