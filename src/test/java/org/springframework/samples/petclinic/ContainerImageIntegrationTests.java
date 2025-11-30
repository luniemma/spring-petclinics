/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DisabledInNativeImage
class ContainerImageIntegrationTests {

	@Test
	void petclinicImageStartsWithDepsAndServesHome() throws Exception {
		String image = System.getProperty("petclinic.image",
				System.getenv().getOrDefault("PETCLINIC_IMAGE",
						"ghcr.io/" + System.getenv().getOrDefault("GITHUB_REPOSITORY", "luniemma/spring-petclinics") + ":latest"));

		try (
				RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));
				KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
				CassandraContainer<?> cassandra = new CassandraContainer<>(DockerImageName.parse("cassandra:4.1"));
				GenericContainer<?> app = new GenericContainer<>(DockerImageName.parse(image)).withExposedPorts(8080)) {

			redis.start();
			kafka.start();
			cassandra.start();

			String redisHost = redis.getHost();
			Integer redisPort = redis.getMappedPort(6379);
			String kafkaBootstrap = kafka.getBootstrapServers();
			String cassandraHost = cassandra.getHost();
			Integer cassandraPort = cassandra.getMappedPort(9042);
			app.withEnv("SPRING_DATA_REDIS_HOST", redisHost)
				.withEnv("SPRING_DATA_REDIS_PORT", String.valueOf(redisPort))
				.withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", kafkaBootstrap)
				.withEnv("SPRING_CASSANDRA_CONTACT_POINTS", cassandraHost)
				.withEnv("SPRING_CASSANDRA_PORT", String.valueOf(cassandraPort))
				.start();

			String baseUrl = "http://" + app.getHost() + ":" + app.getMappedPort(8080);
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/actuator/health")).GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			assertThat(response.statusCode()).isBetween(200, 399);
			assertThat(response.body()).contains("UP");
		}
	}

}
