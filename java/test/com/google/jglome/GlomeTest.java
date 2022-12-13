// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.jglome;

import static com.google.jglome.Glome.MAX_CNT_VALUE;
import static com.google.jglome.Glome.MAX_TAG_LENGTH;
import static com.google.jglome.Glome.MIN_CNT_VALUE;
import static com.google.jglome.Glome.MIN_TAG_LENGTH;
import static com.google.jglome.Glome.PRIVATE_KEY_LENGTH;
import static com.google.jglome.Glome.PUBLIC_KEY_LENGTH;
import static com.google.jglome.TestVector.TEST_VECTORS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.jglome.Glome.CounterOutOfBoundsException;
import com.google.jglome.Glome.GlomeBuilder;
import com.google.jglome.Glome.InvalidKeySize;
import com.google.jglome.Glome.MinPeerTagLengthOutOfBoundsException;
import com.google.jglome.Glome.WrongTagException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Contains tests for com.google.jglome.Glome functionality.
 */
public class GlomeTest {

  public static class KeyPair {

    private final byte[] publicKey;
    private final byte[] privateKey;

    public KeyPair(byte[] publicKey, byte[] privateKey) {
      this.publicKey = publicKey;
      this.privateKey = privateKey;
    }

    public byte[] getPrivateKey() {
      return privateKey;
    }

    public byte[] getPublicKey() {
      return publicKey;
    }
  }

  private static final int N_TEST_VECTORS = TEST_VECTORS.size();

  private final Glome[][] glomeManagers = new Glome[N_TEST_VECTORS][2]; // first is for A, second is for B
  private final KeyPair[] aKeys = new KeyPair[N_TEST_VECTORS];
  private final KeyPair[] bKeys = new KeyPair[N_TEST_VECTORS];

  public GlomeTest() {
    for (int tv = 0; tv < N_TEST_VECTORS; tv++) {
      TestVector testVector = TEST_VECTORS.get(tv);
      aKeys[tv] = new KeyPair(testVector.getKa(), testVector.getKah());
      bKeys[tv] = new KeyPair(testVector.getKb(), testVector.getKbh());
      int finalTV = tv;
      glomeManagers[tv][0] = assertDoesNotThrow(() ->
          new GlomeBuilder(bKeys[finalTV].getPublicKey(), 32)
              .setPrivateKey(aKeys[finalTV].getPrivateKey())
              .build()
      );
      glomeManagers[tv][1] = assertDoesNotThrow(() ->
          new GlomeBuilder(aKeys[finalTV].getPublicKey(), 28)
              .setPrivateKey(bKeys[finalTV].getPrivateKey())
              .build()
      );
    }
  }

  @Test
  public void testShouldFail_whenInvalidKeySizes() {
    InvalidKeySize e1 = assertThrows(
        InvalidKeySize.class,
        () -> new GlomeBuilder(bKeys[0].getPublicKey(), 32)
            .setPrivateKey(Arrays.copyOf(aKeys[1].getPrivateKey(), 31))
            .build()
    );
    InvalidKeySize e2 = assertThrows(
        InvalidKeySize.class,
        () -> new GlomeBuilder(
            Arrays.copyOf(bKeys[0].getPublicKey(), 31), 32
        ).build()
    );

    assertEquals(
        e1.getMessage(),
        String.format(
            "userPrivateKey has invalid size. Expected %d, got %d.",
            PUBLIC_KEY_LENGTH, 31
        )
    );
    assertEquals(
        e2.getMessage(),
        String.format(
            "peerKey has invalid size. Expected %d, got %d.",
            PRIVATE_KEY_LENGTH, 31
        )
    );
  }

  @Test
  public void testShouldFail_whenMinPeerTagLengthIsOutOfBounds() {
    int[] minPeerTagLength = new int[]{MIN_TAG_LENGTH - 1, MAX_TAG_LENGTH + 1};

    for (int len : minPeerTagLength) {
      MinPeerTagLengthOutOfBoundsException e = assertThrows(
          MinPeerTagLengthOutOfBoundsException.class,
          () -> new GlomeBuilder(aKeys[0].getPublicKey(), len),
          String.format(
              "Test testShouldFail_whenMinPeerTagLengthIsOutOfBounds failed: method hasn't thrown an Exception. Loop variable len = %d.",
              len
          )
      );
      assertEquals(
          e.getMessage(),
          String.format(
              "minPeerTagLength argument should be in [%d..%d] range. Got %d.",
              MIN_TAG_LENGTH, MAX_TAG_LENGTH, len
          ),
          String.format(
              "Test testShouldFail_whenMinPeerTagLengthIsOutOfBounds failed: exceptions messages are different. Loop variable len = %d.",
              len
          )
      );
    }
  }

  @Test
  public void checkCorrectMinPeerTagLength() {
    for (int tagLen = MIN_TAG_LENGTH; tagLen <= MAX_TAG_LENGTH; tagLen++) {
      int finalTagLen = tagLen;
      assertDoesNotThrow(() -> new GlomeBuilder(aKeys[0].getPublicKey(), finalTagLen));
    }
  }

  @Test
  public void testShouldFail_whenCounterIsOutOfBounds() {
    TestVector vector = TEST_VECTORS.get(0);
    int[] counters = new int[]{MIN_CNT_VALUE - 1, MAX_CNT_VALUE + 1};

    for (int cnt : counters) {
      CounterOutOfBoundsException e = assertThrows(
          CounterOutOfBoundsException.class,
          () -> glomeManagers[0][0].generateTag(vector.getMsg(), cnt),
          String.format(
              "Test testShouldFail_whenCounterIsOutOfBounds failed: method hasn't thrown an Exception. Loop variable cnt = %d.",
              cnt
          )
      );
      assertEquals(
          e.getMessage(),
          String.format(
              "Counter should be in [%d..%d] range. Got %d.",
              MIN_CNT_VALUE, MAX_CNT_VALUE, cnt
          ),
          String.format(
              "Test testShouldFail_whenCounterIsOutOfBounds failed: exceptions messages are different. Loop variable cnt = %d.",
              cnt
          )
      );
    }
  }

  @Test
  public void checkCorrectCounters() {
    TestVector vector = TEST_VECTORS.get(0);

    for (int cnt = MIN_CNT_VALUE; cnt < MAX_CNT_VALUE; cnt++) {
      int finalCnt = cnt;
      assertDoesNotThrow(
          () -> glomeManagers[0][0].generateTag(vector.getMsg(), finalCnt),
          String.format(
              "Test checkCorrectCounters failed: method has thrown an Exception. Loop variable cnt = %d.",
              cnt
          )
      );
    }
  }

  @Test
  public void derivedKeyShouldEqualOriginalKey() {
    for (int tv = 0; tv < N_TEST_VECTORS; tv++) {
      assertArrayEquals(
          aKeys[tv].getPublicKey(), glomeManagers[tv][0].userPublicKey(),
          String.format(
              "Test derivedKeyShouldEqualOriginalKey failed: keys for A are different. Loop variable tv = %d.",
              tv
          )
      );
      assertArrayEquals(
          bKeys[tv].getPublicKey(), glomeManagers[tv][1].userPublicKey(),
          String.format(
              "Test derivedKeyShouldEqualOriginalKey failed: keys for B are different. Loop variable tv = %d.",
              tv
          )
      );
    }
  }

  @Test
  public void testTagGeneration() {
    for (int tv = 0; tv < N_TEST_VECTORS; tv++) {
      TestVector vector = TEST_VECTORS.get(tv);
      int sender = tv % 2;
      int finalTV = tv;
      assertArrayEquals(
          vector.getTag(),
          assertDoesNotThrow(() ->
              glomeManagers[finalTV][sender].generateTag(vector.getMsg(), vector.getCnt()),
              String.format(
                  "Test testTagGeneration failed: method has thrown an Exception. Loop variable tv = %d.",
                  tv
              )
          )
      );
    }
  }

  @Test
  public void testCheckTag() {
    for (int tv = 0; tv < N_TEST_VECTORS; tv++) {
      TestVector vector = TEST_VECTORS.get(tv);
      int receiver = 1 - tv % 2;
      int finalTV = tv;
      assertDoesNotThrow(() ->
          glomeManagers[finalTV][receiver]
              .checkTag(vector.getTag(), vector.getMsg(), vector.getCnt()),
          String.format(
              "Test testCheckTag failed: method has thrown an Exception. Loop variable tv = %d.",
              tv
          )
      );
    }
  }

  @Test
  public void testCorrectTruncatedTag() {
    TestVector vector = TEST_VECTORS.get(0);
    assertDoesNotThrow(() ->
        glomeManagers[0][1]
            .checkTag(Arrays.copyOf(vector.getTag(), 29), vector.getMsg(), vector.getCnt())
    );
  }

  @Test
  public void testShouldFail_whenIncorrectTruncatedTag() {
    TestVector vector = TEST_VECTORS.get(0);
    byte[] truncatedTag = Arrays.copyOf(vector.getTag(), 29);
    truncatedTag[28] = 0;

    WrongTagException e = assertThrows(
        WrongTagException.class,
        () -> glomeManagers[0][1].checkTag(truncatedTag, vector.getMsg(), vector.getCnt())
    );

    assertEquals("The received tag doesn't match the expected tag.", e.getMessage());
  }

  @Test
  public void testShouldFail_whenInvalidTagLen() {
    TestVector vector = TEST_VECTORS.get(0);
    byte[] truncatedTag = Arrays.copyOf(vector.getTag(), 27);

    WrongTagException e = assertThrows(
        WrongTagException.class,
        () -> glomeManagers[0][1].checkTag(truncatedTag, vector.getMsg(), vector.getCnt())
    );

    assertEquals(
        String.format(
            "The received tag has invalid length. Expected value in range [%d..%d], got %d.",
            28, MAX_TAG_LENGTH, truncatedTag.length),
        e.getMessage()
    );
  }

}
