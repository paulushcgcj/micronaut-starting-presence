package io.github.paulushcgcj;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

  public static boolean await(Duration duration){
   try{ Thread.sleep(duration.toMillis()); }
   catch (InterruptedException e) {  }
    return true;
  }
}
