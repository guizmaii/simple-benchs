package io.simplesource.benchs.gatling.protocol

import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.Clock
import io.gatling.core.action.ExitableAction
import io.gatling.core.session.{Expression, Session}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.simplesource.api.CommandError
import io.simplesource.data.{FutureResult, NonEmptyList, Result}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

/**
 * Inspire by `io.gatling.core.action.RequestAction`
 */
trait SimpleSourceAction[A] extends ExitableAction {
  // FIXME: Maybe not the best EC to use?
  import scala.concurrent.ExecutionContext.Implicits._

  def requestName: Expression[String]
  def sendRequest(requestName: String, session: Session): FutureResult[CommandError, A]
  def ctx: ScenarioContext

  override final def clock: Clock = ctx.coreComponents.clock
  override final def statsEngine: StatsEngine = ctx.coreComponents.statsEngine

  override def execute(session: Session): Unit = recover(session) {
    import io.gatling.commons.util.Throwables._

    import scala.compat.java8.FutureConverters._

    val requestStartDate = clock.nowMillis

    for {
      resolvedRequestName <- requestName(session)
    } yield {
      sendRequest(resolvedRequestName, session).future().toScala.onComplete { response =>
        response match {
          case Failure(e) => statsEngine.reportUnbuildableRequest(session, resolvedRequestName, e.detailedMessage)
          case Success(result: Result[CommandError, A]) =>
            val requestEndDate = clock.nowMillis

            val errorMessage: Option[String] = result.fold(
              (errors: NonEmptyList[CommandError]) => Some(errors.map(_.getMessage()).toList.asScala.mkString(" - ")): Option[String],
              _ => None: Option[String]
            )

            statsEngine.logResponse(
              session,
              resolvedRequestName,
              startTimestamp = requestStartDate,
              endTimestamp = requestEndDate,
              if (result.isSuccess) OK else KO,
              None,
              errorMessage
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
