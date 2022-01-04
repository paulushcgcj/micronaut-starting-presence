package io.github.paulushcgcj.stubs;

import org.junit.jupiter.api.TestInstance;

import io.github.paulushcgcj.grpc.services.PresenceServiceGrpc;
import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import io.micronaut.grpc.server.GrpcServerChannel;
import jakarta.inject.Singleton;

@Factory
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GrpcStubClient {

  @Singleton
  public PresenceServiceGrpc.PresenceServiceBlockingStub blockingStub(
      @GrpcChannel(GrpcServerChannel.NAME) ManagedChannel channel
  ) {
    return PresenceServiceGrpc.newBlockingStub(channel);
  }

}
