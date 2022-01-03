package io.github.paulushcgcj.endpoints;

import io.github.paulushcgcj.configuration.PresenceConfiguration;
import io.github.paulushcgcj.grpc.models.PartyPresence;
import io.github.paulushcgcj.grpc.models.PresenceRequest;
import io.github.paulushcgcj.grpc.models.PresenceStatus;
import io.github.paulushcgcj.grpc.models.UserStatus;
import io.github.paulushcgcj.grpc.services.PresenceServiceGrpc;
import io.github.paulushcgcj.services.PresenceService;
import io.grpc.stub.StreamObserver;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@NoArgsConstructor
public class PresenceEndpoint extends PresenceServiceGrpc.PresenceServiceImplBase {

  public static final String PRESENCE_LOG_MESSAGE = "Updating presence of user {} to {}";
  private PresenceService service;
  private PresenceConfiguration configuration;

  @Inject
  public void setService(PresenceService service) {
    this.service = service;
  }

  @Inject
  public void setConfiguration(PresenceConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void status(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    service.readUserStatus(request.getId())
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
  public void partyStatus(PresenceRequest request, StreamObserver<PartyPresence> responseObserver) {
    service.getPartyStatus(request.getId())
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
  public void onOnline(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    service.updateUserStatus(request.getId(), UserStatus.ONLINE,configuration.getExpiresIn())
        .subscribe(userStatus -> {
          log.info(PRESENCE_LOG_MESSAGE, request.getId(), userStatus.getStatus());
          responseObserver.onNext(userStatus);
          responseObserver.onCompleted();
        });
  }

  @Override
  public void onOffline(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    service.updateUserStatus(request.getId(), UserStatus.OFFLINE,configuration.getExpiresIn())
        .subscribe(userStatus -> {
          log.info(PRESENCE_LOG_MESSAGE, request.getId(), userStatus.getStatus());
          responseObserver.onNext(userStatus);
          responseObserver.onCompleted();
        });
  }

  @Override
  public void onAway(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    service.updateUserStatus(request.getId(), UserStatus.AWAY,configuration.getExpiresIn())
        .subscribe(userStatus -> {
          log.info(PRESENCE_LOG_MESSAGE, request.getId(), userStatus.getStatus());
          responseObserver.onNext(userStatus);
          responseObserver.onCompleted();
        });
  }

  @Override
  public void onBusy(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    service.updateUserStatus(request.getId(), UserStatus.BUSY,configuration.getExpiresIn())
        .subscribe(userStatus -> {
          log.info(PRESENCE_LOG_MESSAGE, request.getId(), userStatus.getStatus());
          responseObserver.onNext(userStatus);
          responseObserver.onCompleted();
        });
  }

  @Override
  public void onOOO(PresenceRequest request, StreamObserver<PresenceStatus> responseObserver) {
    service.updateUserStatus(request.getId(), UserStatus.OUT_OF_OFFICE,configuration.getExpiresIn())
        .subscribe(userStatus -> {
          log.info(PRESENCE_LOG_MESSAGE, request.getId(), userStatus.getStatus());
          responseObserver.onNext(userStatus);
          responseObserver.onCompleted();
        });
  }
}
