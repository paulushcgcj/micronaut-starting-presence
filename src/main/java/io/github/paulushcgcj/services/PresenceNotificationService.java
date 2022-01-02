package io.github.paulushcgcj.services;

import java.time.Duration;

import io.github.paulushcgcj.configuration.PresenceConfiguration;
import io.github.paulushcgcj.grpc.models.UserStatus;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Context
@Slf4j
public class PresenceNotificationService {

  private StatefulRedisPubSubConnection<String,String> connection;
  private PresenceService presenceService;
  private PresenceConfiguration configuration;

  @Inject
  public void setConnection(StatefulRedisPubSubConnection<String, String> connection) {
    this.connection = connection;
  }

  @Inject
  public void setPresenceService(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  @Inject
  public void setConfiguration(PresenceConfiguration configuration) {
    this.configuration = configuration;
  }

  @PostConstruct
  public void checkForNotification(){

    String queueName = String.format("__keyspace@%d__:%s*",configuration.getDatabase(),configuration.getKeyspace());

    connection.addListener(new RedisPubSubAdapter<>(){
      @Override
      public void message(String pattern, String channel, String message) {
        final String key = channel.substring( channel.indexOf(configuration.getKeyspace()) );
        final String id = key.replace(configuration.getKeyspace()+":","");

        presenceService
            .updateUserStatus(id,UserStatus.OFFLINE, Duration.ZERO)
            .subscribe(userStatus -> log.info("User {} session has expired. User now with status {}",id,userStatus.getStatus()));
      }
    });

    log.info("Subscribing to hear notifications at {}",queueName);
    connection
        .reactive()
        .psubscribe(queueName)
        .subscribe();
  }

}
