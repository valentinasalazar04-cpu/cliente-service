package com.denkitronik.clienteservice.domain.services;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import com.denkitronik.clienteservice.domain.repositories.IClienteDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests - ClienteServiceImpl con Mockito")
class ClienteServiceImplTest {

    @Mock
    private IClienteDao clienteDao;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private Cliente cliente;
    private Region region;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setId(1L);
        region.setNombre("Sudamerica");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Linus");
        cliente.setApellido("Torvalds");
        cliente.setEmail("linus@kernel.org");
        cliente.setRegion(region);
    }

    @Test
    @DisplayName("findById - ID existente -> devuelve el cliente")
    void findById_idExistente_debeRetornarCliente() {
        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));

        Cliente resultado = clienteService.findById(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Linus");
        verify(clienteDao, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById - ID inexistente -> lanza ClienteNotFoundException")
    void findById_idInexistente_debeLanzarExcepcion() {
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.findById(999L))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("findAll - devuelve la lista de clientes")
    void findAll_debeRetornarLista() {
        when(clienteDao.findAll()).thenReturn(List.of(cliente));

        List<Cliente> resultado = clienteService.findAll();

        assertThat(resultado).hasSize(1);
        verify(clienteDao, times(1)).findAll();
    }

    @Test
    @DisplayName("findAll(Pageable) - devuelve una pagina")
    void findAllPaginado_debeRetornarPagina() {
        Pageable pageable = PageRequest.of(0, 4);
        when(clienteDao.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(cliente), pageable, 1));

        Page<Cliente> resultado = clienteService.findAll(pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("save - datos validos -> devuelve el cliente guardado")
    void save_datosValidos_debeRetornarCliente() {
        when(clienteDao.save(cliente)).thenReturn(cliente);

        Cliente resultado = clienteService.save(cliente);

        assertThat(resultado.getEmail()).isEqualTo("linus@kernel.org");
        verify(clienteDao, times(1)).save(cliente);
    }

    @Test
    @DisplayName("save - email duplicado -> lanza ClienteServiceException")
    void save_emailDuplicado_debeLanzarClienteServiceException() {
        when(clienteDao.save(cliente))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));

        assertThatThrownBy(() -> clienteService.save(cliente))
                .isInstanceOf(ClienteServiceException.class)
                .hasMessageContaining("duplicados");
    }

    @Test
    @DisplayName("update - ID existente -> actualiza y retorna cliente modificado")
    void update_idExistente_debeActualizarYRetornar() {
        Cliente datosNuevos = new Cliente();
        datosNuevos.setNombre("Linus Updated");
        datosNuevos.setApellido("Torvalds");
        datosNuevos.setEmail("linus2@kernel.org");
        datosNuevos.setRegion(region);

        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteDao.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = clienteService.update(1L, datosNuevos);

        assertThat(resultado.getNombre()).isEqualTo("Linus Updated");
        assertThat(resultado.getEmail()).isEqualTo("linus2@kernel.org");
        verify(clienteDao, times(1)).findById(1L);
        verify(clienteDao, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("update - ID inexistente -> lanza ClienteNotFoundException")
    void update_idInexistente_debeLanzarExcepcion() {
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.update(999L, cliente))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("delete - ID existente -> llama deleteById")
    void delete_idExistente_debeEliminar() {
        when(clienteDao.existsById(1L)).thenReturn(true);

        clienteService.delete(1L);

        verify(clienteDao, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("delete - ID inexistente -> lanza excepcion sin llamar deleteById")
    void delete_idInexistente_noDebeEliminar() {
        when(clienteDao.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> clienteService.delete(999L))
                .isInstanceOf(ClienteNotFoundException.class);

        verify(clienteDao, never()).deleteById(any());
    }

    @Test
    @DisplayName("findAllRegiones - devuelve las regiones")
    void findAllRegiones_debeRetornarRegiones() {
        when(clienteDao.findAllRegiones()).thenReturn(List.of(region));

        List<Region> resultado = clienteService.findAllRegiones();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Sudamerica");
    }
}