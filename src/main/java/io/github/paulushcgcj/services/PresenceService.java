package io.github.paulushcgcj.services;

import io.github.paulushcgcj.grpc.models.PresenceStatus;
import io.github.paulushcgcj.grpc.models.UserStatus;
import io.github.paulushcgcj.utils.RedisUtils;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
@AllArgsConstructor
public class PresenceService {

  private RedisReactiveCommands<String, String> commands;

  @Inject
  public void setCommands(RedisReactiveCommands<String, String> commands) {
    this.commands = commands;
  }

  public Mono<Map<String, PresenceStatus>> getPartyStatus(String partyId) {
    return commands
        .smembers(RedisUtils.buildPartyMembersKey(partyId))
        .flatMap(uid ->
            readUserStatus(uid)
                .map(userStatus ->
                    Pair.of(uid, PresenceStatus.newBuilder().setId(uid).setStatus(userStatus).build())
                )
        )
        .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
  }

  public Mono<PresenceStatus> updateUserStatus(
      String userId,
      UserStatus status,
      Duration expiresIn
  ) {

    Mono<Boolean> expiration = expiresIn.isZero() ?
        Mono.just(true)
        :
        commands.expire(RedisUtils.buildKey(userId), expiresIn);

    log.info("Updating status of user {} to {} expiring at {}", userId, status, expiresIn);
    return commands
        .hset(RedisUtils.buildKey(userId), "status", status.name())
        .flatMap(statusChanged -> expiration)
        .flatMap(statusChanged -> readUserStatus(userId))
        .map(userStatus -> PresenceStatus
            .newBuilder()
            .setStatus(userStatus)
            .setId(userId)
            .build()
        );
  }

  public Mono<UserStatus> readUserStatus(String userId) {
    return commands
        .hget(RedisUtils.buildKey(userId), "status")
        .map(UserStatus::valueOf)
        .switchIfEmpty(Mono.just(UserStatus.UNKNOWN));
  }

}
