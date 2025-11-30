package org.springframework.samples.petclinic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class TestcontainersSmokeTest {

    @Test
    void genericContainerStarts() {
        try (GenericContainer<?> alpine = new GenericContainer<>(DockerImageName.parse("alpine:3.19"))
                .withCommand("sh", "-c", "echo ready && sleep 1")) {
            alpine.start();
            assertThat(alpine.isRunning()).isTrue();
            String logs = alpine.getLogs();
            assertThat(logs).contains("ready");
        }
    }
}
