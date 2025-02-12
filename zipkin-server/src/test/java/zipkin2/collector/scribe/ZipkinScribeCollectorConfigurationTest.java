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
package zipkin2.collector.scribe;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import zipkin2.server.internal.InMemoryConfiguration;
import zipkin2.server.internal.scribe.ZipkinScribeCollectorConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ZipkinScribeCollectorConfigurationTest {
  AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @AfterEach public void close() {
    context.close();
  }

  @Test void doesntProvidesCollectorComponent_byDefault() {
    assertThrows(NoSuchBeanDefinitionException.class, () -> {
      refreshContext();

      context.getBean(ScribeCollector.class);
    });
  }

  /**
   * Note: this will flake if you happen to be running a server on port 9410!
   */
  @Test void providesCollectorComponent_whenEnabled() {
    TestPropertyValues.of("zipkin.collector.scribe.enabled:true").applyTo(context);
    refreshContext();

    assertThat(context.getBean(ScribeCollector.class)).isNotNull();
  }

  @Test void canOverrideProperty_port() {
    TestPropertyValues.of(
      "zipkin.collector.scribe.enabled:true",
      "zipkin.collector.scribe.port:9999")
      .applyTo(context);
    refreshContext();

    assertThat(context.getBean(ScribeCollector.class).server.port)
      .isEqualTo(9999);
  }

  public void refreshContext() {
    context.register(
      PropertyPlaceholderAutoConfiguration.class,
      ZipkinScribeCollectorConfiguration.class,
      InMemoryConfiguration.class);
    context.refresh();
  }
}
