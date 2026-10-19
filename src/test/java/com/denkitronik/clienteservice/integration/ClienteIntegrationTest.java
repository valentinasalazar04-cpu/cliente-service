package com.denkitronik.clienteservice.integration;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E tests - flujo completo HTTP -> H2")
class ClienteIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BASE = "/api/v1/cliente-service";
    private static final AtomicBoolean seeded = new AtomicBoolean(false);
    private static Long regionId;
    private static Long idCreado;

    @BeforeEach
    void seedDatosIniciales() {
        if (seeded.compareAndSet(false, true)) {
            jdbcTemplate.execute(
                "INSERT INTO regiones(nombre) VALUES ('America del Sur')");
            regionId = jdbcTemplate.queryForObject(
                "SELECT id FROM regiones WHERE nombre = 'America del Sur'",
                Long.class);
        }
    }

    @Test @Order(1)
    @DisplayName("POST /clientes -> 201 crea el cliente y retorna el ID")
    void crearCliente_debeRetornar201() {
        Region region = new Region();
        region.setId(regionId);

        Cliente nuevo = new Cliente();
        nuevo.setNombre("Margaret");
        nuevo.setApellido("Hamilton");
        nuevo.setEmail("margaret@apollo.nasa");
        nuevo.setRegion(region);

        ResponseEntity<Cliente> respuesta =
                restTemplate.postForEntity(BASE + "/clientes", nuevo, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().getId()).isNotNull();
        assertThat(respuesta.getBody().getNombre()).isEqualTo("Margaret");

        idCreado = respuesta.getBody().getId();
    }

    @Test @Order(2)
    @DisplayName("GET /clientes/{id} -> 200 encuentra el cliente creado")
    void buscarClienteCreado_debeRetornar200() {
        ResponseEntity<Cliente> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/" + idCreado, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getEmail()).isEqualTo("margaret@apollo.nasa");
    }

    @Test @Order(3)
    @DisplayName("GET /clientes -> 200 la lista contiene al cliente")
    void listarClientes_debeContenerAlCreado() {
        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("Margaret");
    }

    @Test @Order(4)
    @DisplayName("GET /clientes/page/0 -> 200 con content")
    void listarPaginado_debeRetornar200() {
        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/page/0", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("content");
    }

    @Test @Order(5)
    @DisplayName("PUT /clientes/{id} -> 201 actualiza los datos")
    void actualizarCliente_debeRetornar201() {
        Region region = new Region();
        region.setId(regionId);

        Cliente actualizado = new Cliente();
        actualizado.setNombre("Margaret");
        actualizado.setApellido("Hamilton-Updated");
        actualizado.setEmail("margaret@apollo.nasa");
        actualizado.setRegion(region);

        HttpEntity<Cliente> entity = new HttpEntity<>(actualizado, headersJson());

        ResponseEntity<Cliente> respuesta =
                restTemplate.exchange(BASE + "/clientes/" + idCreado,
                        HttpMethod.PUT, entity, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().getApellido()).isEqualTo("Hamilton-Updated");
    }

    @Test @Order(6)
    @DisplayName("GET /clientes/9999 -> 404 ID inexistente")
    void idInexistente_debeRetornar404() {
        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/9999", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).contains("9999");
    }

    @Test @Order(7)
    @DisplayName("DELETE /clientes/{id} -> 204 y luego GET confirma 404")
    void eliminarCliente_debeRetornar204YLuego404() {
        ResponseEntity<Void> borrado =
                restTemplate.exchange(BASE + "/clientes/" + idCreado,
                        HttpMethod.DELETE, null, Void.class);
        assertThat(borrado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/" + idCreado, String.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpHeaders headersJson() {
        HttpHeaders h = new HttpHeaders();
       h.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return h;
    }
}
