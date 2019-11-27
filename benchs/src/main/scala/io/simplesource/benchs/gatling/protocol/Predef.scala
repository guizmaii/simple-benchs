package io.simplesource.benchs.gatling.protocol

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.Protocol
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
