/*
 * Copyright 2017-2020 The OpenTracing Authors
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

package io.opentracing.contrib.kafka;

import static org.junit.Assert.assertEquals;

import java.util.function.BiFunction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;

public class TopicSpanNameTest {

  private static final String EXAMPLE_TOPIC = "example_topic";
  private static final String KAFKA_CLIENT_PREFIX = "KafkaClient: ";
  private static final String UNKNOWN = "unknown";

  private final ConsumerRecord<String, Integer> consumerRecord = new ConsumerRecord<>(
      EXAMPLE_TOPIC, 0, 0, "KEY", 999);
  private final ProducerRecord<String, Integer> producerRecord = new ProducerRecord<>(
      EXAMPLE_TOPIC, 0, System.currentTimeMillis(), "KEY", 999);
  private BiFunction<String, ConsumerRecord, String> consumerSpanNameProvider;
  private BiFunction<String, ProducerRecord, String> producerSpanNameProvider;

  @Test
  public void topicSpanNameTest() {

    consumerSpanNameProvider = ClientSpanNameProvider.CONSUMER_TOPIC;
    producerSpanNameProvider = ClientSpanNameProvider.PRODUCER_TOPIC;

    assertEquals(EXAMPLE_TOPIC, consumerSpanNameProvider.apply("receive", consumerRecord));
    assertEquals(EXAMPLE_TOPIC, producerSpanNameProvider.apply("send", producerRecord));

    assertEquals(EXAMPLE_TOPIC, consumerSpanNameProvider.apply(null, consumerRecord));
    assertEquals(EXAMPLE_TOPIC, producerSpanNameProvider.apply(null, producerRecord));

    assertEquals(UNKNOWN, consumerSpanNameProvider.apply("receive", null));
    assertEquals(UNKNOWN, producerSpanNameProvider.apply("send", null));
  }

  @Test
  public void prefixedTopicSpanNameTest() {
    consumerSpanNameProvider = ClientSpanNameProvider.CONSUMER_PREFIXED_TOPIC(KAFKA_CLIENT_PREFIX);
    producerSpanNameProvider = ClientSpanNameProvider.PRODUCER_PREFIXED_TOPIC(KAFKA_CLIENT_PREFIX);

    assertEquals(KAFKA_CLIENT_PREFIX + EXAMPLE_TOPIC,
        consumerSpanNameProvider.apply("receive", consumerRecord));
    assertEquals(KAFKA_CLIENT_PREFIX + EXAMPLE_TOPIC,
        producerSpanNameProvider.apply("send", producerRecord));

    assertEquals(KAFKA_CLIENT_PREFIX + EXAMPLE_TOPIC,
        consumerSpanNameProvider.apply(null, consumerRecord));
    assertEquals(KAFKA_CLIENT_PREFIX + EXAMPLE_TOPIC,
        producerSpanNameProvider.apply(null, producerRecord));

    assertEquals(KAFKA_CLIENT_PREFIX + UNKNOWN, consumerSpanNameProvider.apply("receive", null));
    assertEquals(KAFKA_CLIENT_PREFIX + UNKNOWN, producerSpanNameProvider.apply("send", null));

    consumerSpanNameProvider = ClientSpanNameProvider.CONSUMER_PREFIXED_TOPIC(null);
    producerSpanNameProvider = ClientSpanNameProvider.PRODUCER_PREFIXED_TOPIC(null);

    assertEquals(EXAMPLE_TOPIC, consumerSpanNameProvider.apply("receive", consumerRecord));
    assertEquals(EXAMPLE_TOPIC, producerSpanNameProvider.apply("send", producerRecord));

    assertEquals(EXAMPLE_TOPIC, consumerSpanNameProvider.apply(null, consumerRecord));
    assertEquals(EXAMPLE_TOPIC, producerSpanNameProvider.apply(null, producerRecord));

    assertEquals(UNKNOWN, consumerSpanNameProvider.apply("receive", null));
    assertEquals(UNKNOWN, producerSpanNameProvider.apply("send", null));
  }
}