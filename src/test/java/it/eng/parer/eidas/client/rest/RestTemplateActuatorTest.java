package it.eng.parer.eidas.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.web.client.RestTemplate;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class RestTemplateActuatorTest {

    final String uri = "http://localhost:8090/actuator";

    RestTemplate restTemplate;

    @BeforeAll
    public void init() {
        restTemplate = new RestTemplate();
    }

    @Test
    @Order(1)
    public void testHealth() throws Exception {
        assertEquals("UP", restTemplate.getForObject(uri + "/health", Map.class).get("status"));

    }

}
