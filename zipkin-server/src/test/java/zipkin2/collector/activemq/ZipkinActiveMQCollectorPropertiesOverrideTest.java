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
package zipkin2.collector.activemq;

import java.util.Arrays;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import zipkin2.server.internal.activemq.Access;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipkinActiveMQCollectorPropertiesOverrideTest {

  AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @AfterEach public void close() {
    context.close();
  }

  public String property;
  public Object value;
  public Function<ActiveMQCollector.Builder, Object> builderExtractor;

  public static Iterable<Object[]> data() {
    return Arrays.asList(
      parameters("url", "failover:(tcp://localhost:61616,tcp://remotehost:61616)",
        b -> b.connectionFactory.getBrokerURL()),
      parameters("client-id-prefix", "zipkin-prod", b -> b.connectionFactory.getClientIDPrefix()),
      parameters("queue", "zapkin", b -> b.queue),
      parameters("concurrency", 2, b -> b.concurrency),
      parameters("username", "u", b -> b.connectionFactory.getUserName()),
      parameters("password", "p", b -> b.connectionFactory.getPassword())
    );
  }

  /** to allow us to define with a lambda */
  static <T> Object[] parameters(
    String propertySuffix, T value, Function<ActiveMQCollector.Builder, T> builderExtractor) {
    return new Object[] {"zipkin.collector.activemq." + propertySuffix, value, builderExtractor};
  }

  @MethodSource("data")
  @ParameterizedTest(name = "{0}")
  void propertyTransferredToCollectorBuilder(String property, Object value,
    Function<ActiveMQCollector.Builder, Object> builderExtractor) {
    initZipkinActiveMQCollectorPropertiesOverrideTest(property, value, builderExtractor);
    if (!property.endsWith("url")) {
      TestPropertyValues.of("zipkin.collector.activemq.url:tcp://localhost:61616").applyTo(context);
    }

    TestPropertyValues.of("zipkin.collector.activemq.$property:$value").applyTo(context);

    if (property.endsWith("username")) {
      TestPropertyValues.of("zipkin.collector.activemq.password:p").applyTo(context);
    }

    if (property.endsWith("password")) {
      TestPropertyValues.of("zipkin.collector.activemq.username:u").applyTo(context);
    }

    TestPropertyValues.of(property + ":" + value).applyTo(context);
    Access.registerActiveMQProperties(context);
    context.refresh();

    assertThat(Access.collectorBuilder(context))
      .extracting(builderExtractor)
      .isEqualTo(value);
  }

  void initZipkinActiveMQCollectorPropertiesOverrideTest(String property, Object value,
    Function<ActiveMQCollector.Builder, Object> builderExtractor) {
    this.property = property;
    this.value = value;
    this.builderExtractor = builderExtractor;
  }
}
