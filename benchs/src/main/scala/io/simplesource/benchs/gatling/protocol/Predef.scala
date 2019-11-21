package io.simplesource.benchs.gatling.protocol

import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ ActorRef, Props }
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.akka.BaseActor
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{ Protocol, ProtocolComponents, ProtocolKey }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.structure.ScenarioContext
import io.simplesource.api.CommandAPI.Request
import io.simplesource.api.{ CommandAPI, CommandError, CommandId }
import io.simplesource.data.{ FutureResult, Sequence }
import io.simplesource.kafka.dsl.EventSourcedApp

import scala.concurrent.duration.FiniteDuration

object Predef extends SimpleSourceDsl

trait SimpleSourceDsl {
  def simpleSource(implicit configuration: GatlingConfiguration): SimpleSourceProtocolBuilder =
    SimpleSourceProtocolBuilder(configuration)

  def stream(name: String): Stream = new Stream(name)
}

final class Stream(actionName: String) {
  def publishCommand[K, C, R](
    requestName: Expression[String]
  )(commandAPI: CommandAPI[K, C], key: K, command: C): ActionBuilder =
    new ActionBuilder {
      private final val safeSequence = new AtomicLong(0)

      override def build(ctx: ScenarioContext, next: Action): Action =
        new SimpleSourceAction[K, C, Request[K, C], CommandId](actionName, requestName, ctx, next) {
          override def sendRequest(
            requestName: String,
            session: Session,
            request: CommandAPI.Request[K, C]
          ): FutureResult[CommandError, CommandId] = commandAPI.publishCommand(request)

          override def newRequest(): Request[K, C] = {
            val r = new CommandAPI.Request[K, C](CommandId.random, key, Sequence.position(safeSequence.getAndIncrement()), command)

            logger.warn(s"============== R: $r")

            r
          }
        }
    }

  def queryCommandResult[K, C](
    requestName: Expression[String]
  )(commandAPI: CommandAPI[K, C], commandId: CommandId, timeout: FiniteDuration): ActionBuilder =
    (ctx: ScenarioContext, next: Action) =>
      new SimpleSourceAction[K, C, (CommandId, Duration), Sequence](actionName, requestName, ctx, next) {
        import scala.compat.java8.DurationConverters._

        private val tupled: ((CommandId, Duration)) => FutureResult[CommandError, Sequence] = (commandAPI.queryCommandResult _).tupled

        override def sendRequest(
          requestName: String,
          session: Session,
          request: (CommandId, Duration)
        ): FutureResult[CommandError, Sequence] = tupled(request)

        override def newRequest(): (CommandId, Duration) = (commandId, timeout.toJava)
      }

  def publishAndQueryCommand[K, C](
    requestName: Expression[String]
  )(commandAPI: CommandAPI[K, C], key: K, command: C, timeout: FiniteDuration): ActionBuilder =
    new ActionBuilder {
      private final val safeSequence = new AtomicLong(0)

      override def build(ctx: ScenarioContext, next: Action): Action =
        new SimpleSourceAction[K, C, Request[K, C], Sequence](actionName, requestName, ctx, next) {
          import scala.compat.java8.DurationConverters._

          override def sendRequest(
            requestName: String,
            session: Session,
            request: CommandAPI.Request[K, C]
          ): FutureResult[CommandError, Sequence] = commandAPI.publishAndQueryCommand(request, timeout.toJava)

          override def newRequest(): CommandAPI.Request[K, C] =
            new CommandAPI.Request[K, C](CommandId.random, key, Sequence.position(safeSequence.getAndIncrement()), command)
        }
    }
}

final case class SimpleSourceProtocolBuilder(
  private val configuration: GatlingConfiguration,
  private val app: Option[EventSourcedApp] = None
) {
  def withApp(app: EventSourcedApp): SimpleSourceProtocolBuilder = copy(app = Some(app))

  def build(): SimpleSourceProtocol = {
    assert(app.nonEmpty, "The app is empty")

    SimpleSourceProtocol { app.get.start(); app.get }
  }
}

final case class SimpleSourceProtocol(app: EventSourcedApp) extends Protocol

final case class SimpleSourceComponents(protocol: SimpleSourceProtocol, sessions: ActorRef) extends ProtocolComponents {
  override def onStart: Session => Session = ProtocolComponents.NoopOnStart
  override def onExit: Session => Unit     = ProtocolComponents.NoopOnExit
}

object SimpleSourceSessions {
  final def props(protocol: SimpleSourceProtocol): Props = Props(new SimpleSourceSessions(protocol))
}

// TODO Jules: Find what this is used for.
class SimpleSourceSessions(protocol: SimpleSourceProtocol) extends BaseActor {
  override def receive: Receive = {
    case _ =>
      val _ = protocol
  }
}

object SimpleSourceProtocol {
  final val simpleSourceProtocolKey: ProtocolKey[SimpleSourceProtocol, SimpleSourceComponents] =
    new ProtocolKey[SimpleSourceProtocol, SimpleSourceComponents] {
      override def protocolClass: Class[Protocol] = classOf[SimpleSourceProtocol].asInstanceOf[Class[Protocol]]

      // TODO Jules: Can we provide defaults for SimpleSource ?
      override def defaultProtocolValue(configuration: GatlingConfiguration): SimpleSourceProtocol = ???

      override def newComponents(
        coreComponents: CoreComponents
      ): SimpleSourceProtocol => SimpleSourceComponents = { protocol =>
        val sessions: ActorRef =
          coreComponents.actorSystem.actorOf(SimpleSourceSessions.props(protocol), "simplesourcesessions")

        SimpleSourceComponents(protocol, sessions)
      }
    }
}
