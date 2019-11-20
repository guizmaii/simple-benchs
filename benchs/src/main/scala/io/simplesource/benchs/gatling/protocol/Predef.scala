package io.simplesource.benchs.gatling.protocol

import akka.actor.{ ActorRef, Props }
import io.gatling.commons.util.Clock
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.akka.BaseActor
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{ Protocol, ProtocolComponents, ProtocolKey }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.simplesource.api.{ CommandAPI, CommandError, CommandId }
import io.simplesource.data.{ FutureResult, Sequence }
import io.simplesource.kafka.dsl.EventSourcedApp
import io.simplesource.kafka.dsl.EventSourcedApp.EventSourcedAppBuilder

import scala.concurrent.duration.FiniteDuration

object Predef extends SimpleSourceDsl

trait SimpleSourceDsl {
  def simpleSource(implicit configuration: GatlingConfiguration): SimpleSourceProtocolBuilder =
    SimpleSourceProtocolBuilder(configuration)

  def stream(name: String): Stream = new Stream(name)

  /**
   * Returns a function for a better type inference at the use location.
   */
  def commandApi[K, C](aggregateName: String): EventSourcedApp => CommandAPI[K, C] =
    app => app.getCommandAPISet(aggregateName).getCommandAPI(aggregateName)
}

final class Stream(name: String) { source =>
  def publishCommand[K, C](
    id: Expression[String]
  )(commandAPI: EventSourcedApp => CommandAPI[K, C], request: CommandAPI.Request[K, C]): ActionBuilder =
    (context: ScenarioContext, nextAction: Action) =>
      new SimpleSourceAction[CommandId] {
        override val requestName: Expression[String] = id
        override val next: Action                    = nextAction
        override val name: String                    = source.name
        override val ctx: ScenarioContext            = context

        override def sendRequest(requestName: String, session: Session): FutureResult[CommandError, CommandId] = {
          val app = ctx.protocolComponentsRegistry.components(SimpleSourceProtocol.simpleSourceProtocolKey).protocol.app

          commandAPI(app).publishCommand(request)
        }
      }

  def queryCommandResult[K, C](
    id: Expression[String]
  )(commandAPI: EventSourcedApp => CommandAPI[K, C], commandId: CommandId, timeout: FiniteDuration): ActionBuilder =
    (context: ScenarioContext, nextAction: Action) =>
      new SimpleSourceAction[Sequence] {
        override val requestName: Expression[String] = id
        override val next: Action                    = nextAction
        override val name: String                    = source.name
        override val ctx: ScenarioContext            = context

        override def sendRequest(requestName: String, session: Session): FutureResult[CommandError, Sequence] = {
          import scala.compat.java8.DurationConverters._
          val app = ctx.protocolComponentsRegistry.components(SimpleSourceProtocol.simpleSourceProtocolKey).protocol.app

          commandAPI(app).queryCommandResult(commandId, timeout.toJava)
        }
      }

  def publishAndQueryCommand[K, C](
    id: Expression[String]
  )(commandAPI: EventSourcedApp => CommandAPI[K, C], request: CommandAPI.Request[K, C], timeout: FiniteDuration): ActionBuilder =
    (context: ScenarioContext, nextAction: Action) =>
      new SimpleSourceAction[Sequence] {
        override val requestName: Expression[String] = id
        override val next: Action                    = nextAction
        override val name: String                    = source.name
        override val ctx: ScenarioContext            = context

        override def sendRequest(requestName: String, session: Session): FutureResult[CommandError, Sequence] = {
          import scala.compat.java8.DurationConverters._
          val app = ctx.protocolComponentsRegistry.components(SimpleSourceProtocol.simpleSourceProtocolKey).protocol.app

          commandAPI(app).publishAndQueryCommand(request, timeout.toJava)
        }
      }
}

final case class SimpleSourceProtocolBuilder(
  private val configuration: GatlingConfiguration,
  private val app: Option[EventSourcedAppBuilder] = None
) {
  def withApp(app: EventSourcedAppBuilder): SimpleSourceProtocolBuilder = copy(app = Some(app))

  def build(): SimpleSourceProtocol = {
    assert(app.nonEmpty, "The app is empty")

    SimpleSourceProtocol(app.get.start())
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

final case class SimpleSourceActionContext(clock: Clock, sessions: ActorRef, statsEngine: StatsEngine, next: Action)

class SimpleSourceActionBuilder extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action = ???
}
