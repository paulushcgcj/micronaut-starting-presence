package io.github.paulushcgcj.services;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.paulushcgcj.TestUtils;
import io.github.paulushcgcj.grpc.models.PresenceStatus;
import io.github.paulushcgcj.grpc.models.UserStatus;
import io.github.paulushcgcj.stubs.RedisDockerStub;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@MicronautTest
@Slf4j
@DisplayName("Integrated Test | Presence Notification Service")
class PresenceNotificationServiceIntegrationTest extends RedisDockerStub {

  @Inject
  private PresenceService presenceService;

  @BeforeAll
  public void setUp() {
    enableKeyspaceNotification();
  }

  @Test
  @DisplayName("Expire After time")
  void shouldExpireAfterTime(){

    String userId = UUID.randomUUID().toString();

    StepVerifier
        .create(presenceService.readUserStatus(userId))
        .expectNext(UserStatus.UNKNOWN)
        .verifyComplete();

    StepVerifier
        .create(presenceService.updateUserStatus(userId,UserStatus.ONLINE, Duration.ofSeconds(10L)))
        .expectNext(PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.ONLINE).build())
        .verifyComplete();

    StepVerifier
        .create(presenceService.readUserStatus(userId))
        .expectNext(UserStatus.ONLINE)
        .verifyComplete();

    await()
        .ignoreExceptions()
        .between(Duration.ofSeconds(10L),Duration.ofSeconds(15L))
        .until(() -> TestUtils.await(Duration.ofSeconds(12L)));

    StepVerifier
        .create(presenceService.readUserStatus(userId))
        .expectNext(UserStatus.OFFLINE)
        .verifyComplete();

  }

}
