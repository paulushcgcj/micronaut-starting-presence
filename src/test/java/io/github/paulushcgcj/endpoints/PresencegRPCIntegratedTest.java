package io.github.paulushcgcj.endpoints;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.paulushcgcj.TestUtils;
import io.github.paulushcgcj.grpc.models.PartyPresence;
import io.github.paulushcgcj.grpc.models.PresenceRequest;
import io.github.paulushcgcj.grpc.models.PresenceStatus;
import io.github.paulushcgcj.grpc.models.UserStatus;
import io.github.paulushcgcj.grpc.services.PresenceServiceGrpc;
import io.github.paulushcgcj.stubs.RedisDockerStub;
import io.github.paulushcgcj.utils.RedisUtils;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@MicronautTest
@Slf4j
@DisplayName("Integration Test | Presence gRPC Endpoing")
class PresencegRPCIntegratedTest extends RedisDockerStub {

  @Inject
  private PresenceServiceGrpc.PresenceServiceBlockingStub client;

  @Inject
  private RedisReactiveCommands<String, String> commands;


  @BeforeAll
  public void warmUp() {
    enableKeyspaceNotification();
  }


  @Test
  @DisplayName("Unknown Users have Unknown status")
  void shouldReceiveStatusUnknownOfInvalidUser(){

    String userId = UUID.randomUUID().toString();

    PresenceStatus expectedStatus = PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.UNKNOWN).build();
    PresenceRequest request = PresenceRequest.newBuilder().setId(userId).build();
    PresenceStatus status = client.status(request);
    assertEquals(expectedStatus,status);
  }

  @Test
  @DisplayName("Set status as online and receive correct status")
  void shouldReceiveStatusAfterSettingToOnline() {

    String userId = UUID.randomUUID().toString();

    PresenceStatus expectedStatus = PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.ONLINE).build();
    PresenceRequest request = PresenceRequest.newBuilder().setId(userId).build();

    checkForStatusChange(userId, expectedStatus, request, r -> client.onOnline(r));
  }

  @Test
  @DisplayName("Set status as offline and receive correct status")
  void shouldReceiveStatusAfterSettingToOffline() {

    String userId = UUID.randomUUID().toString();

    PresenceStatus expectedStatus = PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.OFFLINE).build();
    PresenceRequest request = PresenceRequest.newBuilder().setId(userId).build();
    
    checkForStatusChange(userId, expectedStatus, request, r -> client.onOffline(r));
  }

  @Test
  @DisplayName("Set status as away and receive correct status")
  void shouldReceiveStatusAfterSettingToAway() {

    String userId = UUID.randomUUID().toString();

    PresenceStatus expectedStatus = PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.AWAY).build();
    PresenceRequest request = PresenceRequest.newBuilder().setId(userId).build();
    

    checkForStatusChange(userId, expectedStatus, request, r -> client.onAway(r));
  }

  @Test
  @DisplayName("Set status as Busy and receive correct status")
  void shouldReceiveStatusAfterSettingToBusy() {

    String userId = UUID.randomUUID().toString();

    PresenceStatus expectedStatus = PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.BUSY).build();
    PresenceRequest request = PresenceRequest.newBuilder().setId(userId).build();
    

    checkForStatusChange(userId, expectedStatus, request, r -> client.onBusy(r));
  }

  @Test
  @DisplayName("Set status as Out of Office and receive correct status")
  void shouldReceiveStatusAfterSettingToOutOfOffice() {

    String userId = UUID.randomUUID().toString();

    PresenceStatus expectedStatus = PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.OUT_OF_OFFICE).build();
    PresenceRequest request = PresenceRequest.newBuilder().setId(userId).build();
    

    checkForStatusChange(userId, expectedStatus, request, r -> client.onOOO(r));
  }

  @Test
  @DisplayName("Complete check on User")
  void shouldDoFullInspectionWithExpiration() {

    String userId = UUID.randomUUID().toString();
    PresenceStatus firstExpectedStatus = PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.UNKNOWN).build();
    PresenceStatus secondExpectedStatus = PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.ONLINE).build();
    PresenceStatus thirdExpectedStatus = PresenceStatus.newBuilder().setId(userId).setStatus(UserStatus.OFFLINE).build();
    PresenceRequest request = PresenceRequest.newBuilder().setId(userId).build();

    checkStatus(firstExpectedStatus, userId);
    checkForStatusChange(userId, secondExpectedStatus, request, r -> client.onOnline(r));
    checkStatus(secondExpectedStatus, userId);

    await()
        .ignoreExceptions()
        .between(Duration.ofSeconds(10L), Duration.ofSeconds(15L))
        .until(() -> TestUtils.await(Duration.ofSeconds(12L)));

    checkStatus(thirdExpectedStatus, userId);

  }

  @Test
  @DisplayName("Empty Party")
  void shouldReceiveEmptyParty() {
    String partyId = UUID.randomUUID().toString();
    PartyPresence expectedStatus = PartyPresence.newBuilder().setId(partyId).build();
    PresenceRequest request = PresenceRequest.newBuilder().setId(partyId).build();

    assertEquals(expectedStatus, client.partyStatus(request));
  }

  @Test
  @DisplayName("Full Party")
  void shouldReceiveSomeParty() {
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

    PartyPresence expectedStatus = PartyPresence.newBuilder().setId(partyId).putAllParty(partyStatus).build();
    PresenceRequest request = PresenceRequest.newBuilder().setId(partyId).build();

    assertEquals(expectedStatus, client.partyStatus(request));
  }


  private void checkStatus(PresenceStatus expectedStatus, String userId) {
    PresenceRequest request = PresenceRequest.newBuilder().setId(userId).build();
    PresenceStatus status = client.status(request);
    assertEquals(expectedStatus,status);
  }

  private void checkForStatusChange(
      String userId,
      PresenceStatus expectedStatus,
      PresenceRequest request,
      Function<PresenceRequest,PresenceStatus> func
  ) {
    PresenceStatus received = func.apply(request);
    assertEquals(expectedStatus, received);
    checkStatus(expectedStatus, userId);
  }



}
