package io.github.paulushcgcj.stubs;

import io.github.paulushcgcj.grpc.services.PresenceServiceGrpc;
import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import io.micronaut.grpc.server.GrpcServerChannel;
import org.junit.jupiter.api.TestInstance;

@Factory
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GrpcStubClient {
  public PresenceServiceGrpc.PresenceServiceBlockingStub blockingStub(
      @GrpcChannel(GrpcServerChannel.NAME) ManagedChannel channel
  ) {
    return PresenceServiceGrpc.newBlockingStub(channel);
  }

}
