package io.github.paulushcgcj.services;

import java.time.Duration;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import io.github.paulushcgcj.configuration.PresenceConfiguration;
import io.github.paulushcgcj.grpc.models.PartyPresence;
import io.github.paulushcgcj.grpc.models.PresenceRequest;
import io.github.paulushcgcj.grpc.models.PresenceStatus;
import io.github.paulushcgcj.grpc.models.UserStatus;
import io.github.paulushcgcj.grpc.services.PresenceServiceGrpc;
import io.github.paulushcgcj.utils.RedisUtils;
import io.grpc.stub.StreamObserver;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Singleton
@Slf4j
@AllArgsConstructor
public class PresenceService extends PresenceServiceGrpc.PresenceServiceImplBase {

  private RedisReactiveCommands<String, String> commands;
  private PresenceConfiguration configuration;

  @Inject
  public void setCommands(RedisReactiveCommands<String, String> commands) {
    this.commands = commands;
  }

  @Inject
  public void setConfiguration(PresenceConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void partyStatus(PresenceRequest request, StreamObserver<PartyPresence> responseObserver) {
    commands
        .smembers(RedisUtils.buildPartyMembersKey(request.getId()))
        .flatMap(uid ->
            readUserStatus(uid)
                .map(userStatus ->
                    Pair.of(uid, PresenceStatus.newBuilder().setId(uid).setStatus(userStatus).build())
                )
        )
        .collect(Collectors.toMap(Pair::getLeft, Pair::getRight))
        .map(userStatusMap -> PartyPresence
            .newBuilder()
            .setId(request.getId())
            .putAllParty(userStatusMap)
            .build()
        )
        .subscribe(partyPresence -> {
          responseObserver.onNext(partyPresence);
          responseObserver.onCompleted();
        });
  }

  @Override
  public void status(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {

    readUserStatus(request.getId())
        .map(userStatus -> PresenceStatus
            .newBuilder()
            .setStatus(userStatus)
            .setId(request.getId())
            .build()
        )
        .subscribe(userStatus -> {
          log.info("Requesting presence of user {} :: {}", request.getId(), userStatus.getStatus());
          responseObserver.onNext(userStatus);
          responseObserver.onCompleted();
        });
  }

  @Override
  public void onOnline(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    setUserStatus(request.getId(), UserStatus.ONLINE, responseObserver);
  }

  @Override
  public void onOffline(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    setUserStatus(request.getId(), UserStatus.OFFLINE, responseObserver);
  }

  @Override
  public void onAway(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    setUserStatus(request.getId(), UserStatus.AWAY, responseObserver);
  }

  @Override
  public void onBusy(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    setUserStatus(request.getId(), UserStatus.BUSY,responseObserver);
  }

  @Override
  public void onOOO(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    setUserStatus(request.getId(), UserStatus.OUT_OF_OFFICE, responseObserver);
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

    log.info("Updating status of user {} to {} expiring at {}",userId,status,expiresIn);
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

  private void setUserStatus(
      String userId,
      UserStatus status,
      StreamObserver<PresenceStatus> responseObserver
  ) {
    updateUserStatus(userId, status,configuration.getExpiresIn())
        .subscribe(userStatus -> {
          log.info("Updating presence of user {} to {}", userId, userStatus.getStatus());
          responseObserver.onNext(userStatus);
          responseObserver.onCompleted();
        });
  }

  private Mono<UserStatus> readUserStatus(String userId) {
    return commands
        .hget(RedisUtils.buildKey(userId), "status")
        .map(UserStatus::valueOf)
        .switchIfEmpty(Mono.just(UserStatus.UNKNOWN));
  }

}
