package com.banking.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste E2E de documentação: sobe a aplicação completa em porta aleatória e
 * valida que o Swagger/OpenAPI está publicado com os endpoints esperados.
 *
 * Garante que a documentação não quebre sem o build falhar — se alguém remover
 * o springdoc ou trocar o path do api-docs, este teste acusa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwaggerE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void openApiDocs_deveEstarDisponivelComMetadados() {
        ResponseEntity<String> response = rest.getForEntity(url("/v3/api-docs"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.contains("\"openapi\""), "api-docs deve conter o campo openapi");
        assertTrue(body.contains("Banking Demo API"), "api-docs deve conter o título configurado");
    }

    @Test
    void openApiDocs_deveExporOsEndpointsDoDominio() {
        String body = rest.getForEntity(url("/v3/api-docs"), String.class).getBody();

        assertTrue(body != null, "api-docs não pode ser nulo");
        assertTrue(body.contains("/api/contas"), "api-docs deve documentar /api/contas");
        assertTrue(body.contains("/api/transacoes"), "api-docs deve documentar /api/transacoes");
        assertTrue(body.contains("/api/mercado/selic"), "api-docs deve documentar /api/mercado/selic");
    }

    @Test
    void swaggerUi_deveSerServidoComoHtml() {
        ResponseEntity<String> response = rest.getForEntity(url("/swagger-ui/index.html"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.toLowerCase().contains("swagger"), "swagger-ui deve retornar a página HTML");
    }

    @Test
    void healthCheck_deveEstarUp() {
        ResponseEntity<String> response = rest.getForEntity(url("/actuator/health"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("UP"));
    }

    @Test
    void endopointDeContas_deveResponderPelaRede() {
        ResponseEntity<String> response = rest.getForEntity(url("/api/contas"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("\"success\":true"));
    }
}
