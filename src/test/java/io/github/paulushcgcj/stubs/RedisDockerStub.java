package io.github.paulushcgcj.stubs;

import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
