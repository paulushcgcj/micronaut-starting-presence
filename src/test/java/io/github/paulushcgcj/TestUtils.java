package io.github.paulushcgcj;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

  public static boolean await(Duration duration){
   try{ Thread.sleep(duration.toMillis()); }
   catch (InterruptedException e) {  }
    return true;
  }
}
