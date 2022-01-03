package io.github.paulushcgcj.services;

import io.github.paulushcgcj.TestUtils;
import io.github.paulushcgcj.grpc.models.PresenceStatus;
import io.github.paulushcgcj.grpc.models.UserStatus;
import io.github.paulushcgcj.stubs.RedisDockerStub;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@MicronautTest
@Slf4j
class PresenceServiceIntegrationTest extends RedisDockerStub {

  @Inject
  private PresenceService service;

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
  void shouldReturnStatusAfterSet(){
    String userId = UUID.randomUUID().toString();

    StepVerifier
        .create(service.readUserStatus(userId))
        .expectNext(UserStatus.UNKNOWN)
        .verifyComplete();

    StepVerifier
        .create(service.updateUserStatus(userId,UserStatus.ONLINE, Duration.ZERO))
        .expectNext(PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.ONLINE).build())
        .verifyComplete();

    StepVerifier
        .create(service.readUserStatus(userId))
        .expectNext(UserStatus.ONLINE)
        .verifyComplete();
  }

  @Test
  @DisplayName("Expire After time")
  void shouldExpireAfterTime(){
    String userId = UUID.randomUUID().toString();

    StepVerifier
        .create(service.readUserStatus(userId))
        .expectNext(UserStatus.UNKNOWN)
        .verifyComplete();

    StepVerifier
        .create(service.updateUserStatus(userId,UserStatus.ONLINE, Duration.ofSeconds(10L)))
        .expectNext(PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.ONLINE).build())
        .verifyComplete();

    StepVerifier
        .create(service.readUserStatus(userId))
        .expectNext(UserStatus.ONLINE)
        .verifyComplete();

    await()
        .ignoreExceptions()
        .between(Duration.ofSeconds(10L),Duration.ofSeconds(15L))
        .until(() -> TestUtils.await(Duration.ofSeconds(12L)));

    StepVerifier
        .create(service.readUserStatus(userId))
        .expectNext(UserStatus.UNKNOWN)
        .verifyComplete();

  }



}