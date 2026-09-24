package com.fablab.rh.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.rh.dto.PessoaRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.repository.PessoaRepository;
import com.fablab.rh.service.PessoaService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {

    @Mock
    private PessoaRepository pessoaRepository;

    @InjectMocks
    private PessoaService pessoaService;

    private Pessoa pessoa(long id, String nome, String matricula) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(id);
        pessoa.setNomeCompleto(nome);
        pessoa.setMatricula(matricula);
        pessoa.setStatus(PessoaStatus.ATIVO);
        return pessoa;
    }

    private PessoaRequest request(String nome, String matricula) {
        return new PessoaRequest(nome, matricula, null, null, null, null, null);
    }

    @Test
    void cadastrarComSucesso() {
        when(pessoaRepository.existsByMatricula("M1")).thenReturn(false);
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(inv -> {
            Pessoa p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        var response = pessoaService.cadastrar(request("Maria", "M1"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.nomeCompleto()).isEqualTo("Maria");
        assertThat(response.matricula()).isEqualTo("M1");
        assertThat(response.status()).isEqualTo(PessoaStatus.ATIVO);
        verify(pessoaRepository).save(any(Pessoa.class));
    }

    @Test
    void cadastrarRejeitaMatriculaDuplicada() {
        Pessoa existente = pessoa(5, "Outra", "M1");
        when(pessoaRepository.existsByMatricula("M1")).thenReturn(true);
        when(pessoaRepository.findByMatricula("M1")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> pessoaService.cadastrar(request("Maria", "M1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Matrícula já cadastrada");
    }

    @Test
    void buscarDevePermitirAdmin() {
        Pessoa pessoa = pessoa(1, "Maria", "M1");
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));

        var response = pessoaService.buscar(1L, principalAdmin());

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void buscarDevePermitirProprioUsuario() {
        Pessoa pessoa = pessoa(1, "Maria", "M1");
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));

        var response = pessoaService.buscar(1L, new RhPrincipal(1L, 7L, NivelAcesso.BOLSISTA, "FabLab"));

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void buscarRejeitaAcessoDeOutroUsuario() {
        Pessoa pessoa = pessoa(1, "Maria", "M1");
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));

        RhPrincipal principal = new RhPrincipal(99L, 7L, NivelAcesso.ESTAGIARIO, "FabLab");

        assertThatThrownBy(() -> pessoaService.buscar(1L, principal))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void buscarLancaNotFoundQuandoNaoExiste() {
        when(pessoaRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pessoaService.buscar(777L, principalAdmin()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atualizarPreservaMatriculaDoProprioRegistro() {
        Pessoa pessoa = pessoa(1, "Maria", "M1");
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(pessoaRepository.existsByMatricula("M1")).thenReturn(true);
        when(pessoaRepository.findByMatricula("M1")).thenReturn(Optional.of(pessoa));
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = pessoaService.atualizar(1L, request("Maria Silvia", "M1"), principalAdmin());

        assertThat(response.nomeCompleto()).isEqualTo("Maria Silvia");
    }

    private RhPrincipal principalAdmin() {
        return new RhPrincipal(1L, 1L, NivelAcesso.ADMIN, "FabLab");
    }
}