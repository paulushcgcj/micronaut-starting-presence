package io.github.paulushcgcj.configuration;

import java.time.Duration;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.*;

@ConfigurationProperties("io.github.paulushcgcj.presence")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@With
public class PresenceConfiguration {
  @Builder.Default
  private Duration expiresIn = Duration.ofMinutes(2L);
  @Builder.Default
  private int database = 0;
  @Builder.Default
  private String keyspace = "io:github:paulushcgcj:uid";
}
