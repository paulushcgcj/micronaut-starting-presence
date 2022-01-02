package io.github.paulushcgcj.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisUtils {
  public static String buildKey(String uid) {
    return String.format("io:github:paulushcgcj:uid:%s", uid);
  }

  public static String buildPartyMembersKey(String uid) {
    return String.format("io:github:paulushcgcj:pid:%s:members", uid);
  }

}
