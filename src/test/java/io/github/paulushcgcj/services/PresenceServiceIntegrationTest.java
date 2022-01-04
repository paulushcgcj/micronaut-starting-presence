package io.github.paulushcgcj.services;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.paulushcgcj.TestUtils;
import io.github.paulushcgcj.grpc.models.PresenceStatus;
import io.github.paulushcgcj.grpc.models.UserStatus;
import io.github.paulushcgcj.stubs.RedisDockerStub;
import io.github.paulushcgcj.utils.RedisUtils;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@MicronautTest
@Slf4j
@DisplayName("Integrated Test | Presence Service")
class PresenceServiceIntegrationTest extends RedisDockerStub {

  @Inject
  private PresenceService service;
  @Inject
  private RedisReactiveCommands<String, String> commands;

  @BeforeAll
  public void setUp() {
    disableKeyspaceNotification();
  }

  @Test
  @DisplayName("Unknown for user not found")
  void shouldReturnUnknownStatusWhenUserDoesNotExist() {
    StepVerifier
        .create(service.readUserStatus(UUID.randomUUID().toString()))
        .expectNext(UserStatus.UNKNOWN)
        .verifyComplete();
  }

  @Test
  @DisplayName("From Unknown to Online")
  void shouldReturnStatusAfterSet() {
    String userId = UUID.randomUUID().toString();

    StepVerifier
        .create(service.readUserStatus(userId))
        .expectNext(UserStatus.UNKNOWN)
        .verifyComplete();

    StepVerifier
        .create(service.updateUserStatus(userId, UserStatus.ONLINE, Duration.ZERO))
        .expectNext(PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.ONLINE).build())
        .verifyComplete();

    StepVerifier
        .create(service.readUserStatus(userId))
        .expectNext(UserStatus.ONLINE)
        .verifyComplete();
  }

  @Test
  @DisplayName("Expire After time")
  void shouldExpireAfterTime() {
    String userId = UUID.randomUUID().toString();

    StepVerifier
        .create(service.readUserStatus(userId))
        .expectNext(UserStatus.UNKNOWN)
        .verifyComplete();

    StepVerifier
        .create(service.updateUserStatus(userId, UserStatus.ONLINE, Duration.ofSeconds(10L)))
        .expectNext(PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.ONLINE).build())
        .verifyComplete();

    StepVerifier
        .create(service.readUserStatus(userId))
        .expectNext(UserStatus.ONLINE)
        .verifyComplete();

    await()
        .ignoreExceptions()
        .between(Duration.ofSeconds(10L), Duration.ofSeconds(15L))
        .until(() -> TestUtils.await(Duration.ofSeconds(12L)));

    StepVerifier
        .create(service.readUserStatus(userId))
        .expectNext(UserStatus.UNKNOWN)
        .verifyComplete();

  }


  @Test
  @DisplayName("Empty Map for non-existing parties")
  void shouldGiveEmptyMapForPartyNotFound() {
    StepVerifier
        .create(service.getPartyStatus(UUID.randomUUID().toString()))
        .expectNext(new HashMap<>())
        .verifyComplete();

  }

  @Test
  @DisplayName("Presence for Party")
  void shouldGivePresenceForExistingParty() {

    String partyId = UUID.randomUUID().toString();

    Map<String, PresenceStatus> partyStatus =
        IntStream
            .range(0, 5)
            .mapToObj(x -> UUID.randomUUID().toString())
            .map(userId -> {
              commands.sadd(RedisUtils.buildPartyMembersKey(partyId), userId).subscribe();
              commands.hset(RedisUtils.buildKey(userId), "status", "ONLINE").subscribe();
              return Pair.of(userId, PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.ONLINE).build());
            })
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

    StepVerifier
        .create(service.getPartyStatus(partyId))
        .expectNext(partyStatus)
        .verifyComplete();

  }


}
