package io.simplesource.benchs.it.example

import java.util.Optional

import io.simplesource.api.CommandAPI
import io.simplesource.example.user.domain.{ User, UserCommand, UserEvent, UserKey }
import io.simplesource.kafka.client.{ CommandAPIBuilder, EventSourcedClient }
import io.simplesource.kafka.dsl.KafkaConfig.Builder
import io.simplesource.kafka.dsl.{ AggregateBuilder, EventSourcedApp }
import io.simplesource.kafka.serialization.json.JsonAggregateSerdes
import io.simplesource.kafka.serialization.json.JsonGenericMapper.jsonDomainMapper
import io.simplesource.kafka.serialization.json.JsonOptionalGenericMapper.jsonOptionalDomainMapper
import io.simplesource.kafka.util.PrefixResourceNamingStrategy

object Config {
  // The following code is copied from here:
  //  - https://github.com/simplesourcing/simplesource-examples/blob/master/examples/user/src/main/java/io/simplesource/example/user/json/UserJsonRunner.java

  val aggregateName: String = "benchs-aggregate"

  val namingStrategy: PrefixResourceNamingStrategy = new PrefixResourceNamingStrategy("user_json_")

  val aggregateSerdes: JsonAggregateSerdes[UserKey, UserCommand, UserEvent, Optional[User]] =
    new JsonAggregateSerdes(jsonDomainMapper(), jsonDomainMapper(), jsonDomainMapper(), jsonOptionalDomainMapper())

  val app: EventSourcedApp =
    new EventSourcedApp().withKafkaConfig { builder: Builder =>
      builder
        .withKafkaApplicationId("benchsApp")
        .withKafkaBootstrap("localhost:9092")
        .build()
    }.withAggregate { builder: AggregateBuilder[UserKey, UserCommand, UserEvent, Optional[User]] =>
      builder
        .withName(aggregateName)
        .withSerdes(aggregateSerdes)
        .withResourceNamingStrategy(namingStrategy)
        .withInitialValue(_ => Optional.empty())
        .withAggregator(UserEvent.getAggregator)
        .withCommandHandler(UserCommand.getCommandHandler)
        .build()
    }

  val client: EventSourcedClient =
    new EventSourcedClient().withKafkaConfig { builder =>
      builder
        .withKafkaBootstrap("localhost:9092")
        .build()
    }

  val commandAPI: CommandAPI[UserKey, UserCommand] = client.createCommandApi { builder: CommandAPIBuilder[UserKey, UserCommand] =>
    builder
      .withClientId("benchsClient")
      .withName(aggregateName)
      .withSerdes(aggregateSerdes)
      .withResourceNamingStrategy(namingStrategy)
      .build()
  }
}
