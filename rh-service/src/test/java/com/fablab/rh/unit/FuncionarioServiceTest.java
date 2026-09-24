package com.fablab.rh.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fablab.rh.dto.FuncionarioRequest;
import com.fablab.rh.dto.NivelRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.ApontamentoHoras;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.HistoricoNivel;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.entity.RegistroPontoDiario;
import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.entity.TipoApontamento;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.repository.ApontamentoHorasRepository;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.HistoricoNivelRepository;
import com.fablab.rh.repository.PessoaRepository;
import com.fablab.rh.repository.RegistroPontoDiarioRepository;
import com.fablab.rh.service.FuncionarioService;
import com.fablab.rh.service.RhEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private HistoricoNivelRepository historicoRepository;
    @Mock
    private RegistroPontoDiarioRepository pontoRepository;
    @Mock
    private ApontamentoHorasRepository apontamentoRepository;
    @Mock
    private RhEventPublisher eventPublisher;

    @InjectMocks
    private FuncionarioService funcionarioService;

    private Pessoa pessoa(long id, String nome) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(id);
        pessoa.setNomeCompleto(nome);
        pessoa.setMatricula("M" + id);
        pessoa.setStatus(PessoaStatus.ATIVO);
        return pessoa;
    }

    private Funcionario funcionario(long id, Pessoa pessoa, NivelAcesso nivel) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setPessoa(pessoa);
        funcionario.setNivelAcesso(nivel);
        return funcionario;
    }

    @Test
    void vincularComSucesso() {
        Pessoa pessoa = pessoa(1, "Maria");
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(funcionarioRepository.existsByPessoaId(1L)).thenReturn(false);
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> {
            Funcionario f = inv.getArgument(0);
            f.setId(2L);
            return f;
        });

        var response = funcionarioService.vincular(new FuncionarioRequest(1L, NivelAcesso.BOLSISTA, "Eletrônica"));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.idPessoa()).isEqualTo(1L);
        assertThat(response.nivelAcesso()).isEqualTo(NivelAcesso.BOLSISTA);
    }

    @Test
    void vincularLancaNotFoundQuandoPessoaInexistente() {
        when(pessoaRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> funcionarioService.vincular(new FuncionarioRequest(9L, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void vincularRejeitaPessoaJaVinculada() {
        Pessoa pessoa = pessoa(1, "Maria");
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(funcionarioRepository.existsByPessoaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> funcionarioService.vincular(new FuncionarioRequest(1L, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já vinculada");
    }

    @Test
    void alterarNivelRegistraHistoricoEPublicaEvento() {
        Pessoa pessoa = pessoa(1, "Maria");
        Funcionario funcionario = funcionario(7, pessoa, NivelAcesso.BOLSISTA);
        Funcionario admin = funcionario(1, pessoa(2, "Admin"), NivelAcesso.ADMIN);
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        var response = funcionarioService.alterarNivel(
                7L, new NivelRequest(NivelAcesso.ESTAGIARIO), principalAdmin());

        assertThat(response.nivelAntigo()).isEqualTo(NivelAcesso.BOLSISTA);
        assertThat(response.nivelNovo()).isEqualTo(NivelAcesso.ESTAGIARIO);
        verify(historicoRepository).save(any(HistoricoNivel.class));
        verify(eventPublisher).publishNivelAlterado(any());
    }

    @Test
    void alterarNivelMesmoNivelNaoPublicaEventoNemRegistraHistorico() {
        Pessoa pessoa = pessoa(1, "Maria");
        Funcionario funcionario = funcionario(7, pessoa, NivelAcesso.ADMIN);
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));

        var response = funcionarioService.alterarNivel(
                7L, new NivelRequest(NivelAcesso.ADMIN), principalAdmin());

        assertThat(response.nivelNovo()).isEqualTo(NivelAcesso.ADMIN);
        verify(historicoRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void totalHorasSomaPresencaEncomendaEProjeto() {
        Pessoa pessoa = pessoa(1, "Maria");
        Funcionario funcionario = funcionario(7, pessoa, NivelAcesso.BOLSISTA);
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));

        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setData(LocalDate.of(2026, 1, 5));
        registro.setTotalHoras(new BigDecimal("8.00"));
        when(pontoRepository.findByFuncionarioId(7L)).thenReturn(List.of(registro));

        ApontamentoHoras encomenda = apontamento(TipoApontamento.ENCOMENDA, "3.00");
        ApontamentoHoras projeto = apontamento(TipoApontamento.PROJETO, "2.00");
        when(apontamentoRepository.findByFuncionarioId(7L)).thenReturn(List.of(encomenda, projeto));

        var response = funcionarioService.totalHoras(7L, new RhPrincipal(1L, 7L, NivelAcesso.BOLSISTA, "FabLab"));

        assertThat(response.totalHorasPresenca()).isEqualByComparingTo("8.00");
        assertThat(response.totalHorasEncomenda()).isEqualByComparingTo("3.00");
        assertThat(response.totalHorasProjeto()).isEqualByComparingTo("2.00");
        assertThat(response.incoerencias()).isEmpty();
    }

    @Test
    void totalHorasApontaIncoerenciaDoDia() {
        Pessoa pessoa = pessoa(1, "Maria");
        Funcionario funcionario = funcionario(7, pessoa, NivelAcesso.BOLSISTA);
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));

        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setData(LocalDate.of(2026, 1, 5));
        registro.setTotalHoras(new BigDecimal("4.00"));
        when(pontoRepository.findByFuncionarioId(7L)).thenReturn(List.of(registro));

        ApontamentoHoras encomenda = apontamento(TipoApontamento.ENCOMENDA, "3.00");
        ApontamentoHoras projeto = apontamento(TipoApontamento.PROJETO, "2.00");
        when(apontamentoRepository.findByFuncionarioId(7L)).thenReturn(List.of(encomenda, projeto));

        var response = funcionarioService.totalHoras(7L, new RhPrincipal(1L, 7L, NivelAcesso.BOLSISTA, "FabLab"));

        assertThat(response.incoerencias()).hasSize(1);
        assertThat(response.incoerencias().get(0).horasApontadas()).isEqualByComparingTo("5.00");
        assertThat(response.incoerencias().get(0).horasPresenca()).isEqualByComparingTo("4.00");
    }

    @Test
    void totalHorasRejeitaAcessoDeOutroFuncionario() {
        Pessoa pessoa = pessoa(1, "Maria");
        Funcionario funcionario = funcionario(7, pessoa, NivelAcesso.BOLSISTA);
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));

        RhPrincipal principal = new RhPrincipal(99L, 99L, NivelAcesso.BOLSISTA, "FabLab");

        assertThatThrownBy(() -> funcionarioService.totalHoras(7L, principal))
                .isInstanceOf(ForbiddenException.class);
    }

    private ApontamentoHoras apontamento(TipoApontamento tipo, String horas) {
        ApontamentoHoras apontamento = new ApontamentoHoras();
        apontamento.setTipo(tipo);
        apontamento.setStatus(StatusApontamento.VALIDADO);
        apontamento.setHorasTrabalhadas(new BigDecimal(horas));
        apontamento.setData(LocalDate.of(2026, 1, 5));
        return apontamento;
    }

    private RhPrincipal principalAdmin() {
        return new RhPrincipal(1L, 1L, NivelAcesso.ADMIN, "FabLab");
    }
}