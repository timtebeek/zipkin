/*
 * Copyright 2015-2023 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin2.internal;

import java.io.IOException;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static zipkin2.internal.JsonCodec.exceptionReading;

public class JsonCodecTest {

  @Test void doesntStackOverflowOnToBufferWriterBug_lessThanBytes() {
    Throwable exception = assertThrows(AssertionError.class, () -> {

      class FooWriter implements WriteBuffer.Writer {
        @Override public int sizeInBytes(Object value) {
          return 2;
        }

        @Override public void write(Object value, WriteBuffer buffer) {
          buffer.writeByte('a');
          throw new RuntimeException("buggy");
        }
      }

      class Foo {
        @Override public String toString() {
          return new String(JsonCodec.write(new FooWriter(), this), UTF_8);
        }
      }

      new Foo().toString(); // cause the exception
    });
    assertTrue(exception.getMessage()
      .contains(
        "Bug found using FooWriter to write Foo as json. Wrote 1/2 bytes: a")); // cause the exception
  }

  @Test void doesntStackOverflowOnToBufferWriterBug_Overflow() {
    Throwable exception = assertThrows(AssertionError.class, () -> {

      // pretend there was a bug calculating size, ex it calculated incorrectly as to small
      class FooWriter implements WriteBuffer.Writer {
        @Override public int sizeInBytes(Object value) {
          return 2;
        }

        @Override public void write(Object value, WriteBuffer buffer) {
          buffer.writeByte('a');
          buffer.writeByte('b');
          buffer.writeByte('c'); // wrote larger than size!
        }
      }

      class Foo {
        @Override public String toString() {
          return new String(JsonCodec.write(new FooWriter(), this), UTF_8);
        }
      }

      new Foo().toString(); // cause the exception
    });
    assertTrue(exception.getMessage()
      .contains(
        "Bug found using FooWriter to write Foo as json. Wrote 2/2 bytes: ab")); // cause the exception
  }

  @Test void exceptionReading_malformedJsonWraps() {
    // grab a real exception from the gson library
    Exception error = null;
    byte[] bytes = "[\"='".getBytes(UTF_8);
    try {
      new JsonCodec.JsonReader(ReadBuffer.wrap(bytes)).beginObject();
      failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IOException | IllegalStateException e) {
      error = e;
    }

    try {
      exceptionReading("List<Span>", error);
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Malformed reading List<Span> from json");
    }
  }
}
