package com.fablab.rh.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.rh.dto.ApontamentoHorasRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.ValidacaoApontamentoRequest;
import com.fablab.rh.entity.ApontamentoHoras;
import com.fablab.rh.entity.Funcionario;
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
import com.fablab.rh.repository.RegistroPontoDiarioRepository;
import com.fablab.rh.service.ApontamentoHorasService;
import com.fablab.rh.service.RhEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApontamentoHorasServiceTest {

    @Mock
    private ApontamentoHorasRepository apontamentoRepository;
    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private RegistroPontoDiarioRepository pontoRepository;
    @Mock
    private RhEventPublisher eventPublisher;

    @InjectMocks
    private ApontamentoHorasService apontamentoService;

    private final LocalDate data = LocalDate.of(2026, 1, 5);

    private Funcionario funcionario(long id, long idPessoa) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(idPessoa);
        pessoa.setNomeCompleto("Maria");
        pessoa.setMatricula("M" + idPessoa);
        pessoa.setStatus(PessoaStatus.ATIVO);
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setPessoa(pessoa);
        funcionario.setNivelAcesso(NivelAcesso.BOLSISTA);
        return funcionario;
    }

    @Test
    void registrarComSucessoParaSiMesmo() {
        Funcionario funcionario = funcionario(7, 7);
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));
        when(apontamentoRepository.save(any(ApontamentoHoras.class))).thenAnswer(inv -> {
            ApontamentoHoras a = inv.getArgument(0);
            a.setId(11L);
            return a;
        });

        var response = apontamentoService.registrar(
                new ApontamentoHorasRequest(7L, TipoApontamento.PROJETO, 3L, data, new BigDecimal("2.00"), null, null, null),
                new RhPrincipal(7L, 7L, NivelAcesso.BOLSISTA, "FabLab"));

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.status()).isEqualTo(StatusApontamento.PENDENTE);
    }

    @Test
    void adminPodeRegistrarParaOutroFuncionario() {
        Funcionario funcionario = funcionario(7, 7);
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));
        when(apontamentoRepository.save(any(ApontamentoHoras.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = apontamentoService.registrar(
                new ApontamentoHorasRequest(7L, TipoApontamento.ENCOMENDA, 3L, data, new BigDecimal("2.00"), null, null, null),
                principalAdmin());

        assertThat(response.idFuncionario()).isEqualTo(7L);
    }

    @Test
    void registrarRejeitaParaOutroFuncionario() {
        Funcionario funcionario = funcionario(7, 7);
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));

        RhPrincipal principal = new RhPrincipal(999L, 999L, NivelAcesso.VOLUNTARIO, "FabLab");

        assertThatThrownBy(() -> apontamentoService.registrar(
                new ApontamentoHorasRequest(7L, TipoApontamento.PROJETO, 3L, data, new BigDecimal("1.00"), null, null, null),
                principal))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void validarPublicaEventoDeHorasValidadas() {
        Funcionario funcionario = funcionario(7, 7);
        ApontamentoHoras apontamento = apontamentoPendente(funcionario);
        when(apontamentoRepository.findById(11L)).thenReturn(Optional.of(apontamento));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario(1, 1)));

        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setTotalHoras(new BigDecimal("8.00"));
        when(pontoRepository.findByFuncionarioIdAndData(7L, data)).thenReturn(Optional.of(registro));
        when(apontamentoRepository.sumHorasApontadasPorDia(eq(7L), eq(data))).thenReturn(new BigDecimal("2.00"));
        when(apontamentoRepository.save(any(ApontamentoHoras.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = apontamentoService.validar(
                11L, new ValidacaoApontamentoRequest(StatusApontamento.VALIDADO, null), principalAdmin());

        assertThat(response.status()).isEqualTo(StatusApontamento.VALIDADO);
        ArgumentCaptor<com.fablab.rh.dto.HorasValidadasEvent> captor =
                ArgumentCaptor.forClass(com.fablab.rh.dto.HorasValidadasEvent.class);
        verify(eventPublisher).publishHorasValidadas(captor.capture());
        assertThat(captor.getValue().idFuncionario()).isEqualTo(7L);
        assertThat(captor.getValue().horas()).isEqualByComparingTo("2.00");
    }

    @Test
    void validarRejeitadoNaoPublicaEvento() {
        Funcionario funcionario = funcionario(7, 7);
        ApontamentoHoras apontamento = apontamentoPendente(funcionario);
        when(apontamentoRepository.findById(11L)).thenReturn(Optional.of(apontamento));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario(1, 1)));
        when(apontamentoRepository.save(any(ApontamentoHoras.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = apontamentoService.validar(
                11L, new ValidacaoApontamentoRequest(StatusApontamento.REJEITADO, null), principalAdmin());

        assertThat(response.status()).isEqualTo(StatusApontamento.REJEITADO);
        verify(eventPublisher, never()).publishHorasValidadas(any());
    }

    @Test
    void validarRejeitaHorasSemPresencaRegistrada() {
        Funcionario funcionario = funcionario(7, 7);
        ApontamentoHoras apontamento = apontamentoPendente(funcionario);
        when(apontamentoRepository.findById(11L)).thenReturn(Optional.of(apontamento));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario(1, 1)));
        when(pontoRepository.findByFuncionarioIdAndData(7L, data)).thenReturn(Optional.empty());
        when(apontamentoRepository.sumHorasApontadasPorDia(eq(7L), eq(data))).thenReturn(new BigDecimal("2.00"));

        assertThatThrownBy(() -> apontamentoService.validar(
                11L, new ValidacaoApontamentoRequest(StatusApontamento.VALIDADO, null), principalAdmin()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("presença");
    }

    @Test
    void validarRejeitaHorasQueExcedemAPresenca() {
        Funcionario funcionario = funcionario(7, 7);
        ApontamentoHoras apontamento = apontamentoPendente(funcionario);
        when(apontamentoRepository.findById(11L)).thenReturn(Optional.of(apontamento));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario(1, 1)));

        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setTotalHoras(new BigDecimal("4.00"));
        when(pontoRepository.findByFuncionarioIdAndData(7L, data)).thenReturn(Optional.of(registro));
        when(apontamentoRepository.sumHorasApontadasPorDia(eq(7L), eq(data))).thenReturn(new BigDecimal("9.00"));

        assertThatThrownBy(() -> apontamentoService.validar(
                11L, new ValidacaoApontamentoRequest(StatusApontamento.VALIDADO, null), principalAdmin()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("excedem");
    }

    @Test
    void validarJaEstabelecidoLancaIllegalArgument() {
        Funcionario funcionario = funcionario(7, 7);
        ApontamentoHoras apontamento = apontamentoPendente(funcionario);
        apontamento.setStatus(StatusApontamento.VALIDADO);
        when(apontamentoRepository.findById(11L)).thenReturn(Optional.of(apontamento));

        assertThatThrownBy(() -> apontamentoService.validar(
                11L, new ValidacaoApontamentoRequest(StatusApontamento.VALIDADO, null), principalAdmin()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já validado");
    }

    @Test
    void validarLancaNotFound() {
        when(apontamentoRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apontamentoService.validar(
                999L, new ValidacaoApontamentoRequest(StatusApontamento.VALIDADO, null), principalAdmin()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ApontamentoHoras apontamentoPendente(Funcionario funcionario) {
        ApontamentoHoras apontamento = new ApontamentoHoras();
        apontamento.setId(11L);
        apontamento.setFuncionario(funcionario);
        apontamento.setTipo(TipoApontamento.PROJETO);
        apontamento.setIdReferencia(3L);
        apontamento.setData(data);
        apontamento.setHorasTrabalhadas(new BigDecimal("2.00"));
        apontamento.setStatus(StatusApontamento.PENDENTE);
        return apontamento;
    }

    private RhPrincipal principalAdmin() {
        return new RhPrincipal(1L, 1L, NivelAcesso.ADMIN, "FabLab");
    }
}