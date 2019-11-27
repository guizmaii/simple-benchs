package io.simplesource.benchs.it.example

import java.util.Optional

import com.typesafe.config.ConfigFactory
import io.simplesource.api.CommandAPI
import io.simplesource.example.user.avro.UserAvroMappers.{ aggregateMapper, commandMapper, eventMapper, keyMapper }
import io.simplesource.example.user.domain.{ User, UserCommand, UserEvent, UserKey }
import io.simplesource.kafka.api.{ AggregateSerdes, ResourceNamingStrategy }
import io.simplesource.kafka.client.{ CommandAPIBuilder, EventSourcedClient }
import io.simplesource.kafka.dsl.KafkaConfig.Builder
import io.simplesource.kafka.dsl.{ AggregateBuilder, EventSourcedApp }
import io.simplesource.kafka.serialization.avro.AvroSerdes
import io.simplesource.kafka.serialization.json.JsonAggregateSerdes
import io.simplesource.kafka.util.PrefixResourceNamingStrategy

object Config {
  // The following code is inspired by:
  //  - https://github.com/simplesourcing/simplesource-examples/blob/master/examples/user/src/main/java/io/simplesource/example/user

  private val config           = ConfigFactory.load("benchs.conf")
  private val bootstrapServers = config.getString("benchs.kafka.bootstrap-servers")
  private val schemaRegistry   = config.getString("benchs.kafka.schema-registry")

  private val jsonAggregateName: String                                                                 = "json-benchs-aggregate"
  private val jsonNamingStrategy: PrefixResourceNamingStrategy                                          = new PrefixResourceNamingStrategy("user_json_")
  private val jsonAggregateSerdes: JsonAggregateSerdes[UserKey, UserCommand, UserEvent, Optional[User]] = new JsonAggregateSerdes()

  private val avroAggregateName: String                        = "avro-benchs-aggregate"
  private val avroNamingStrategy: PrefixResourceNamingStrategy = new PrefixResourceNamingStrategy("user_avro_");
  private val avroAggregateSerdes: AggregateSerdes[UserKey, UserCommand, UserEvent, Optional[User]] =
    AvroSerdes.Custom.aggregate(
      keyMapper,
      commandMapper,
      eventMapper,
      aggregateMapper,
      schemaRegistry,
      false,
      io.simplesource.example.user.avro.api.User.SCHEMA$
    )

  private def appAndClient(
    applicationId: String,
    clientId: String,
    aggregateName: String,
    namingStrategy: ResourceNamingStrategy,
    serdes: AggregateSerdes[UserKey, UserCommand, UserEvent, Optional[User]]
  ): (EventSourcedApp, CommandAPI[UserKey, UserCommand]) = {
    val app: EventSourcedApp =
      new EventSourcedApp().withKafkaConfig { builder: Builder =>
        builder
          .withKafkaApplicationId(applicationId)
          .withKafkaBootstrap(bootstrapServers)
          .build()
      }.withAggregate { builder: AggregateBuilder[UserKey, UserCommand, UserEvent, Optional[User]] =>
        builder
          .withName(aggregateName)
          .withSerdes(serdes)
          .withResourceNamingStrategy(namingStrategy)
          .withInitialValue(_ => Optional.empty())
          .withAggregator(UserEvent.getAggregator)
          .withCommandHandler(UserCommand.getCommandHandler)
          .build()
      }

    val commandAPI: CommandAPI[UserKey, UserCommand] =
      new EventSourcedClient()
        .withKafkaConfig(_.withKafkaBootstrap(bootstrapServers).build())
        .createCommandAPI { builder: CommandAPIBuilder[UserKey, UserCommand] =>
          builder
            .withClientId(clientId)
            .withName(aggregateName)
            .withSerdes(serdes)
            .withResourceNamingStrategy(namingStrategy)
            .build()
        }

    (app, commandAPI)
  }

  def jsonAppAndClient: (EventSourcedApp, CommandAPI[UserKey, UserCommand]) =
    appAndClient("json-user-benchs-app", "json-user-benchs-client", jsonAggregateName, jsonNamingStrategy, jsonAggregateSerdes)

  def avroAppAndClient: (EventSourcedApp, CommandAPI[UserKey, UserCommand]) =
    appAndClient("avro-user-benchs-app", "avro-user-benchs-client", avroAggregateName, avroNamingStrategy, avroAggregateSerdes)
}
