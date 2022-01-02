package io.github.paulushcgcj.configuration;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class RedisConfiguration {

  @Singleton
  public RedisReactiveCommands<String, String> commands(
      StatefulRedisConnection<String, String> connection
  ){
    return connection.reactive();
  }
}
