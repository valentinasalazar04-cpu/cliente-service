package com.denkitronik.clienteservice.domain.repositories;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("Integration tests (data slice) - IClienteDao con H2")
class IClienteDaoTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private IClienteDao clienteDao;

    private Region region;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setNombre("Asia");
        em.persist(region);

        cliente = new Cliente();
        cliente.setNombre("Grace");
        cliente.setApellido("Hopper");
        cliente.setEmail("grace@navy.mil");
        cliente.setRegion(region);
        em.persist(cliente);

        em.flush();
    }

    @Test
    @DisplayName("findAllRegiones - devuelve las regiones persistidas")
    void findAllRegiones_debeRetornarListaDeRegiones() {
        List<Region> regiones = clienteDao.findAllRegiones();

        assertThat(regiones).hasSize(1);
        assertThat(regiones.get(0).getNombre()).isEqualTo("Asia");
    }

    @Test
    @DisplayName("findById - ID existente -> devuelve el cliente")
    void findById_existente_debeRetornarCliente() {
        Optional<Cliente> resultado = clienteDao.findById(cliente.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEmail()).isEqualTo("grace@navy.mil");
    }

    @Test
    @DisplayName("findById - ID inexistente -> Optional vacio")
    void findById_inexistente_debeRetornarVacio() {
        assertThat(clienteDao.findById(9999L)).isEmpty();
    }

    @Test
    @DisplayName("save - asigna id y createAt con @PrePersist")
    void save_debeAsignarIdYCreateAt() {
        Cliente nuevo = new Cliente();
        nuevo.setNombre("Ada");
        nuevo.setApellido("Lovelace");
        nuevo.setEmail("ada@babbage.io");
        nuevo.setRegion(region);

        Cliente guardado = clienteDao.save(nuevo);
        em.flush();

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getCreateAt()).isNotNull();
    }

    @Test
    @DisplayName("findAll(Pageable) - devuelve una pagina")
    void findAllPaginado_debeRetornarPagina() {
        Page<Cliente> pagina = clienteDao.findAll(PageRequest.of(0, 4));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("deleteById - elimina el cliente verificado con em.find")
    void deleteById_debeEliminarElCliente() {
        Long id = cliente.getId();

        clienteDao.deleteById(id);
        em.flush();

        assertThat(em.find(Cliente.class, id)).isNull();
    }
}
