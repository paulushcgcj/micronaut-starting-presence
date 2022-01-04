package io.github.paulushcgcj.utils;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Test | Redis Utils")
class RedisUtilsTest {

  @Test
  @DisplayName("UserID Key")
  void shouldGiveExpectedUIDKey(){
    String uid = UUID.randomUUID().toString();
    assertEquals(
        String.format("io:github:paulushcgcj:uid:%s",uid),
        RedisUtils.buildKey(uid)
    );
  }

  @Test
  @DisplayName("Party Members pid")
  void shouldGiveExpectedPIDKey(){
    String pid = UUID.randomUUID().toString();
    assertEquals(
        String.format("io:github:paulushcgcj:pid:%s:members",pid),
        RedisUtils.buildPartyMembersKey(pid)
    );
  }

}
