package io.github.paulushcgcj.stubs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestResponseObserver<T> implements StreamObserver<T> {
  private final CountDownLatch finishLatch = new CountDownLatch(1);
  private final AtomicReference<T> atomicReference = new AtomicReference<>();

  @Override
  public void onNext(T status) {
    log.info("Received a status as {}", status);
    atomicReference.set(status);
  }

  @Override
  public void onError(Throwable t) {
    log.error("Recorded a failure", t);
    finishLatch.countDown();
  }

  @Override
  public void onCompleted() {
    log.info("Finished Observer");
    finishLatch.countDown();
  }

  public CountDownLatch getFinishLatch() {
    return finishLatch;
  }

  public T getReceivedStatus() {
    return atomicReference.get();
  }
}
