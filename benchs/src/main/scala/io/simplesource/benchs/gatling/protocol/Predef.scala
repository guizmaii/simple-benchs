package io.simplesource.benchs.gatling.protocol

import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{ Protocol, ProtocolComponents, ProtocolKey }
import io.gatling.core.session.Session
import io.simplesource.kafka.dsl.EventSourcedApp

object Predef extends SimpleSourceDsl

trait SimpleSourceDsl {
  def simpleSource(implicit configuration: GatlingConfiguration): SimpleSourceProtocolBuilder =
    SimpleSourceProtocolBuilder(configuration)

  def stream(name: String): Stream = new Stream(name)
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

object SimpleSourceProtocol {
  final val simpleSourceProtocolKey: ProtocolKey[SimpleSourceProtocol, SimpleSourceComponents] =
    new ProtocolKey[SimpleSourceProtocol, SimpleSourceComponents] {
      override def protocolClass: Class[Protocol] = classOf[SimpleSourceProtocol].asInstanceOf[Class[Protocol]]

      // TODO Jules: Can we provide defaults for SimpleSource ?
      override def defaultProtocolValue(configuration: GatlingConfiguration): SimpleSourceProtocol =
        throw new IllegalStateException("Can't provide a default value for SimpleSource")

      override def newComponents(coreComponents: CoreComponents): SimpleSourceProtocol => SimpleSourceComponents =
        protocol => SimpleSourceComponents(protocol)
    }
}

final case class SimpleSourceComponents(protocol: SimpleSourceProtocol) extends ProtocolComponents {
  override def onStart: Session => Session = ProtocolComponents.NoopOnStart
  override def onExit: Session => Unit     = ProtocolComponents.NoopOnExit
}
