package com.fablab.vendas.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.vendas.dto.ClienteRequest;
import com.fablab.vendas.dto.ClienteResponse;
import com.fablab.vendas.dto.TagClienteRequest;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.TagCliente;
import com.fablab.vendas.entity.TipoPessoa;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.ClienteTagRepository;
import com.fablab.vendas.repository.TagClienteRepository;
import com.fablab.vendas.service.ClienteService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TagClienteRepository tagClienteRepository;
    @Mock
    private ClienteTagRepository clienteTagRepository;

    private ClienteService service;

    @BeforeEach
    void setUp() {
        service = new ClienteService(clienteRepository, tagClienteRepository, clienteTagRepository);
    }

    private Cliente clienteValido() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setTipoPessoa(TipoPessoa.PF);
        cliente.setNomeRazaoSocial("João");
        cliente.setCpfCnpj("52998224725");
        cliente.setDataCadastro(LocalDate.now());
        return cliente;
    }

    @Test
    void criarPersisteClienteValidoComDataCadastro() {
        ClienteRequest request = new ClienteRequest(
                TipoPessoa.PF, "João", "52998224725", "j@x.com", null, null, null);
        when(clienteRepository.existsByCpfCnpj("52998224725")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        ClienteResponse response = service.criar(request);

        assertEquals("João", response.nomeRazaoSocial());
        assertTrue(response.dataCadastro() != null);
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void criarRejeitaCpfInvalido() {
        ClienteRequest request = new ClienteRequest(
                TipoPessoa.PF, "João", "12345678900", null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.criar(request));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void criarRejeitaCpfDuplicado() {
        ClienteRequest request = new ClienteRequest(
                TipoPessoa.PF, "João", "52998224725", null, null, null, null);
        when(clienteRepository.existsByCpfCnpj("52998224725")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.criar(request));
    }

    @Test
    void atualizarAlteraCampos() {
        Cliente cliente = clienteValido();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        ClienteRequest request = new ClienteRequest(
                TipoPessoa.PF, "João da Silva", "52998224725", "novo@x.com", null, null, List.of());
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        ClienteResponse response = service.atualizar(1L, request);

        assertEquals("João da Silva", response.nomeRazaoSocial());
        assertEquals("novo@x.com", response.email());
    }

    @Test
    void atualizarRejeitaDocumentoDeOutroCliente() {
        var cliente = clienteValido();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByCpfCnpj("11222333000181")).thenReturn(true);
        ClienteRequest request = new ClienteRequest(
                TipoPessoa.PJ, "Empresa", "11222333000181", null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(1L, request));
    }

    @Test
    void atualizarClienteInexistenteLancaNotFound() throws Exception {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());
        ClienteRequest request = new ClienteRequest(
                TipoPessoa.PF, "X", "52998224725", null, null, null, null);
        assertThrows(ResourceNotFoundException.class, () -> service.atualizar(99L, request));
    }

    @Test
    void adicionarTagJaExistenteLancaErro() {
        var cliente = clienteValido();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(tagClienteRepository.findById(5L)).thenReturn(Optional.of(novaTag()));
        when(clienteTagRepository.existsByCliente_IdAndTag_Id(1L, 5L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.adicionarTag(1L, 5L));
    }

    @Test
    void criarTagDuplicadaLancaErro() {
        when(tagClienteRepository.findByNomeIgnoreCase("VIP")).thenReturn(Optional.of(novaTag()));
        assertThrows(IllegalArgumentException.class,
                () -> service.criarTag(new TagClienteRequest("VIP", "#fff")));
    }

    @Test
    void criarTagNovaPersiste() {
        when(tagClienteRepository.findByNomeIgnoreCase("3D")).thenReturn(Optional.empty());
        when(tagClienteRepository.save(any(TagCliente.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.criarTag(new TagClienteRequest("3D", "#0f0"));

        assertEquals("3D", response.nome());
    }

    private TagCliente novaTag() {
        TagCliente tag = new TagCliente();
        tag.setId(5L);
        tag.setNome("VIP");
        return tag;
    }
}