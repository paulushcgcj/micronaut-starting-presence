package io.github.paulushcgcj.stubs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.micronaut.test.support.TestPropertyProvider;
import lombok.extern.slf4j.Slf4j;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public abstract class RedisDockerStub extends GenericContainer<RedisDockerStub> implements TestPropertyProvider {

  private static final String REDIS_IMAGE = "redis:5-alpine";
  private static final int REDIS_PORT = 6379;

  @Container
  public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
      .withExposedPorts(REDIS_PORT);

  @Override
  public void start() {
    super.start();
    System.setProperty("REDIS_URI",getRedisAddress());
  }

  public static void enableKeyspaceNotification() {
    try { log.info("Executed config set notify-keyspace-events Kxe and got {}",redis.execInContainer("redis-cli","config", "set", "notify-keyspace-events", "Kxe").getStdout()); }
    catch (IOException |InterruptedException e) {
      log.error("Error during post start command execution on redis",e);
    }
  }


  public static void disableKeyspaceNotification() {
    try { log.info("Executed config set notify-keyspace-events and got {}",redis.execInContainer("redis-cli","config", "set", "notify-keyspace-events", "").getStdout()); }
    catch (IOException |InterruptedException e) {
      log.error("Error during post start command execution on redis",e);
    }
  }

  @Override
  public void stop() {
    super.stop();
    log.info("Stopping redis execution");
  }

  @Override
  public Map<String, String> get() {
    Map<String,String> props = new HashMap<>(TestPropertyProvider.super.get());
    props.putAll(Map.of("redis.uri", String.format("redis://%s:%d",redis.getHost(), redis.getMappedPort(REDIS_PORT))));
    return props;
  }

  @Override
  public Map<String, String> getProperties() {
    return Map.of("redis.uri", String.format("redis://%s:%d",redis.getHost(), redis.getMappedPort(REDIS_PORT)));
  }

  private String getRedisAddress(){
    return String.format("redis://%s:%d",redis.getContainerIpAddress(), redis.getFirstMappedPort());
  }
}
