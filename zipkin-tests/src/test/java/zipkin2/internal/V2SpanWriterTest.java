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

import org.junit.jupiter.api.Test;
import zipkin2.Endpoint;
import zipkin2.Span;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static zipkin2.TestObjects.CLIENT_SPAN;
import static zipkin2.TestObjects.TODAY;

public class V2SpanWriterTest {
  V2SpanWriter writer = new V2SpanWriter();
  byte[] bytes = new byte[2048]; // bigger than needed to test sizeInBytes
  WriteBuffer buf = WriteBuffer.wrap(bytes);

  @Test void sizeInBytes() {
    writer.write(CLIENT_SPAN, buf);
    assertThat(writer.sizeInBytes(CLIENT_SPAN))
      .isEqualTo(buf.pos());
  }

  @Test void writes128BitTraceId() {
    writer.write(CLIENT_SPAN, buf);

    assertThat(new String(bytes, UTF_8))
      .startsWith("{\"traceId\":\"" + CLIENT_SPAN.traceId() + "\"");
  }

  @Test void writesAnnotationWithoutEndpoint() {
    writer.write(CLIENT_SPAN, buf);

    assertThat(new String(bytes, UTF_8))
      .contains("{\"timestamp\":" + (TODAY + 100) * 1000L + ",\"value\":\"foo\"}");
  }

  @Test void omitsEmptySpanName() {
    Span span = Span.newBuilder()
      .traceId("7180c278b62e8f6a216a2aea45d08fc9")
      .parentId("6b221d5bc9e6496c")
      .id("5b4185666d50f68b")
      .build();

    writer.write(span, buf);

    assertThat(new String(bytes, UTF_8))
      .doesNotContain("name");
  }

  @Test void omitsEmptyServiceName() {
    Span span = CLIENT_SPAN.toBuilder()
      .localEndpoint(Endpoint.newBuilder().ip("127.0.0.1").build())
      .build();

    writer.write(span, buf);

    assertThat(new String(bytes, UTF_8))
      .contains("\"localEndpoint\":{\"ipv4\":\"127.0.0.1\"}");
  }

  @Test void tagsAreAMap() {
    writer.write(CLIENT_SPAN, buf);

    assertThat(new String(bytes, UTF_8))
      .contains("\"tags\":{\"clnt/finagle.version\":\"6.45.0\",\"http.path\":\"/api\"}");
  }
}
