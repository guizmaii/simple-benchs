package io.simplesource.benchs.it

import java.util

import com.typesafe.config.{ Config, ConfigFactory }

object BenchConfigs {

  private val config: Config = ConfigFactory.load("benchs.conf")
  private val benchConfigs   = config.getConfig("io.simplesource.benchs")

  val kafkaBrokers: util.List[String] = benchConfigs.getStringList("kafka.brokers")

}
