package io.simplesource.benchs.gatling.protocol

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.Clock
import io.gatling.core.action.{ Action, ExitableAction }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.simplesource.api.CommandError
import io.simplesource.data.{ FutureResult, NonEmptyList, Result }

import scala.collection.JavaConverters._

/**
 * Inspired by `io.gatling.core.action.RequestAction`
 */
abstract class SimpleSourceAction[K, C, P, A](
  actionName: String,
  requestName: Expression[String],
  ctx: ScenarioContext,
  override final val next: Action
) extends ExitableAction {
  def requestParamsGen(): P
  def sendRequest(requestName: String, session: Session, requestParameters: P): FutureResult[CommandError, A]

  override final def name: String             = actionName
  override final def clock: Clock             = ctx.coreComponents.clock
  override final def statsEngine: StatsEngine = ctx.coreComponents.statsEngine

  override final def execute(session: Session): Unit = recover(session) {
    import io.gatling.commons.util.Throwables._

    for {
      resolvedRequestName <- requestName(session)
    } yield {
      val parameters: P    = requestParamsGen()
      val requestStartDate = clock.nowMillis

      sendRequest(resolvedRequestName, session, parameters).future().whenComplete { (result: Result[CommandError, A], e: Throwable) =>
        val requestEndDate = clock.nowMillis

        if (e ne null) {
          statsEngine.logCrash(session, resolvedRequestName, e.detailedMessage)
        } else {
          val errorMessage: Option[String] = result.fold(
            (errors: NonEmptyList[CommandError]) => Some(errors.map(_.getMessage()).toList.asScala.mkString(" - ")): Option[String],
            _ => None: Option[String]
          )

          statsEngine.logResponse(
            session = session,
            requestName = resolvedRequestName,
            startTimestamp = requestStartDate,
            endTimestamp = requestEndDate,
            status = if (result.isSuccess) OK else KO,
            responseCode = None,
            message = errorMessage
          )
        }

        if (ctx.throttled) {
          ctx.coreComponents.throttler.throttle(session.scenario, () => next ! session)
        } else {
          next ! session
        }
      }
    }
  }
}
