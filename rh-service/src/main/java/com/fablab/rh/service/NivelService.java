package com.fablab.rh.service;

import com.fablab.rh.dto.ConviteRequest;
import com.fablab.rh.dto.FuncionarioRequest;
import com.fablab.rh.dto.FuncionarioResponse;
import com.fablab.rh.dto.NivelMembroRequest;
import com.fablab.rh.dto.NivelRequest;
import com.fablab.rh.dto.NivelResponse;
import com.fablab.rh.dto.NiveisResponse;
import com.fablab.rh.dto.PermissaoNivelResponse;
import com.fablab.rh.dto.PessoaRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.repository.FuncionarioRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Níveis de acesso: matriz de permissões (espelho do enforcement real),
 * alteração de membro e convites.
 */
@Service
public class NivelService {

    private static final String TOTAL = "TOTAL";
    private static final String PROPRIO = "PROPRIO";
    private static final String NAO = "NAO";

    private final FuncionarioRepository funcionarioRepository;
    private final PessoaService pessoaService;
    private final FuncionarioService funcionarioService;

    public NivelService(FuncionarioRepository funcionarioRepository,
                        PessoaService pessoaService,
                        FuncionarioService funcionarioService) {
        this.funcionarioRepository = funcionarioRepository;
        this.pessoaService = pessoaService;
        this.funcionarioService = funcionarioService;
    }

    /** Matriz de níveis com membros e capacidades aplicadas no servidor. */
    @Transactional(readOnly = true)
    public NiveisResponse matriz() {
        Map<NivelAcesso, Long> contagem = new EnumMap<>(NivelAcesso.class);
        for (Object[] linha : funcionarioRepository.contarPorNivel()) {
            contagem.put((NivelAcesso) linha[0], (Long) linha[1]);
        }
        List<NivelResponse> niveis = List.of(NivelAcesso.values()).stream()
                .map(n -> new NivelResponse(
                        n.name().toLowerCase(), n.getCode(), n.getLabel(),
                        contagem.getOrDefault(n, 0L)))
                .toList();
        return new NiveisResponse(niveis, List.of(
                permissao("pessoas", "Pessoas", TOTAL, TOTAL, TOTAL, PROPRIO, NAO),
                permissao("funcionarios", "Funcionários e horas", TOTAL, PROPRIO, PROPRIO, PROPRIO, NAO),
                permissao("alterar-nivel", "Alterar nível", TOTAL, NAO, NAO, NAO, NAO),
                permissao("apontamentos", "Registrar horas", TOTAL, PROPRIO, PROPRIO, NAO, NAO),
                permissao("validar-horas", "Validar horas", TOTAL, NAO, NAO, NAO, NAO),
                permissao("processo-seletivo", "Processo seletivo", TOTAL, PROPRIO, PROPRIO, NAO, NAO),
                permissao("treinamentos", "Treinamentos", TOTAL, PROPRIO, PROPRIO, NAO, NAO),
                permissao("certificados", "Certificados", TOTAL, PROPRIO, PROPRIO, PROPRIO, NAO),
                permissao("decidir-certificados", "Aprovar certificados", TOTAL, NAO, NAO, NAO, NAO)));
    }

    private PermissaoNivelResponse permissao(String id, String rotulo, String admin, String bolsista,
                                             String voluntario, String estagiario, String recrutando) {
        return new PermissaoNivelResponse(id, rotulo, admin, bolsista, voluntario, estagiario,
                recrutando);
    }

    /** Altera o nível do membro (delega com histórico e evento). */
    @Transactional
    public com.fablab.rh.dto.NivelAlteradoResponse alterarMembro(Long idFuncionario,
                                                                 NivelMembroRequest request,
                                                                 RhPrincipal principal) {
        return funcionarioService.alterarNivel(
                idFuncionario, new NivelRequest(request.nivel()), principal);
    }

    /**
     * Convida um membro: cria a pessoa (Recrutando quando nível 4) e vincula
     * o funcionário com o nível inicial.
     */
    @Transactional
    public FuncionarioResponse convidar(ConviteRequest request, RhPrincipal principal) {
        NivelAcesso nivel = request.nivelAcesso() == null ? NivelAcesso.BOLSISTA : request.nivelAcesso();
        PessoaStatus status = nivel == NivelAcesso.RECRUTANDO ? PessoaStatus.RECRUTANDO : PessoaStatus.ATIVO;
        var pessoa = pessoaService.cadastrar(new PessoaRequest(
                request.nomeCompleto(), request.matricula(), null,
                request.contato(), null, status, null));
        return funcionarioService.vincular(
                new FuncionarioRequest(pessoa.id(), nivel, request.departamento()));
    }
}
