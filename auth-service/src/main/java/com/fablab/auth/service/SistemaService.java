package com.fablab.auth.service;

import com.fablab.auth.dto.Sistema;
import com.fablab.auth.entity.ParametroSistema;
import com.fablab.auth.entity.TokenIntegracao;
import com.fablab.auth.exception.ConfiguracaoInvalidaException;
import com.fablab.auth.repository.ParametroSistemaRepository;
import com.fablab.auth.repository.TokenIntegracaoRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parâmetros globais do sistema (KV {@code parametro_sistema}).
 *
 * <p>Só persiste identidade + cadências; alimenta Produção (cadência 5S)
 * por leitura — sem chamar outro service.</p>
 */
@Service
public class SistemaService {

    static final String K_NOME = "identidade.nomeFablab";
    static final String K_LOGO = "identidade.logo";
    static final String K_CAD_CHECKLIST = "cadencia.checklist5S";
    static final String K_CAD_AUDITORIA = "cadencia.auditoria5S";

    private static final Map<String, String> DEFAULTS = Map.of(
            K_NOME, "FabLab IFPR — Curitiba",
            K_LOGO, "",
            K_CAD_CHECKLIST, "Semanal",
            K_CAD_AUDITORIA, "Mensal");

    /** Cadências aceitas (mockup cfg-sistema: Semanal/Quinzenal/Mensal). */
    private static final Set<String> CADENCIAS = Set.of("SEMANAL", "QUINZENAL", "MENSAL");

    private final ParametroSistemaRepository parametroRepository;
    private final TokenIntegracaoRepository tokenRepository;

    public SistemaService(ParametroSistemaRepository parametroRepository,
                          TokenIntegracaoRepository tokenRepository) {
        this.parametroRepository = parametroRepository;
        this.tokenRepository = tokenRepository;
    }

    private void ensureDefaults() {
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (!parametroRepository.existsById(entry.getKey())) {
                parametroRepository.save(new ParametroSistema(entry.getKey(), entry.getValue()));
            }
        }
    }

    private String valor(String chave) {
        return parametroRepository.findById(chave)
                .map(ParametroSistema::getValor)
                .orElse(DEFAULTS.get(chave));
    }

    /**
     * Parâmetros atuais + tokens ativos (sem segredo).
     */
    @Transactional
    public Sistema.SistemaResponse obter() {
        ensureDefaults();
        List<Sistema.TokenResumoResponse> tokens = tokenRepository.findByRevogadoFalseOrderByCriadoEmDesc()
                .stream().map(this::resumo).toList();
        String auditoria = valor(K_CAD_AUDITORIA);
        return new Sistema.SistemaResponse(
                new Sistema.IdentidadeResponse(valor(K_NOME), valor(K_LOGO)),
                valor(K_CAD_CHECKLIST),
                (auditoria == null || auditoria.isBlank()) ? null : auditoria,
                tokens);
    }

    /**
     * Persiste identidade + cadências (validação PT; 422 se inválido).
     */
    @Transactional
    public Sistema.SistemaResponse atualizar(Sistema.AtualizarSistemaRequest request) {
        ensureDefaults();
        if (request == null || request.identidade() == null) {
            throw new ConfiguracaoInvalidaException("Identidade é obrigatória");
        }
        String nome = request.identidade().nomeFablab() == null
                ? "" : request.identidade().nomeFablab().trim();
        if (nome.isEmpty()) {
            throw new ConfiguracaoInvalidaException("Nome do laboratório é obrigatório");
        }
        if (nome.length() > 255) {
            throw new ConfiguracaoInvalidaException("Nome do laboratório deve ter no máximo 255 caracteres");
        }
        String logo = request.identidade().logo() == null ? "" : request.identidade().logo().trim();
        if (logo.length() > 2048) {
            throw new ConfiguracaoInvalidaException("Logo deve ter no máximo 2048 caracteres");
        }
        String checklist = normalizarCadencia(request.cadenciaChecklist5S(),
                "Cadência do checklist 5S é obrigatória",
                "Cadência do checklist 5S inválida: use Semanal, Quinzenal ou Mensal");
        String auditoria = null;
        if (request.cadenciaAuditoria5S() != null && !request.cadenciaAuditoria5S().isBlank()) {
            auditoria = normalizarCadencia(request.cadenciaAuditoria5S(), null,
                    "Cadência da auditoria 5S inválida: use Semanal, Quinzenal ou Mensal");
        }
        salvar(K_NOME, nome);
        salvar(K_LOGO, logo);
        salvar(K_CAD_CHECKLIST, checklist);
        salvar(K_CAD_AUDITORIA, auditoria == null ? "" : auditoria);
        return obter();
    }

    private String normalizarCadencia(String raw, String msgObrigatorio, String msgInvalida) {
        if (raw == null || raw.isBlank()) {
            throw new ConfiguracaoInvalidaException(msgObrigatorio);
        }
        String normalized = raw.trim().toUpperCase();
        if (!CADENCIAS.contains(normalized)) {
            throw new ConfiguracaoInvalidaException(msgInvalida);
        }
        return normalized.charAt(0) + normalized.substring(1).toLowerCase();
    }

    private void salvar(String chave, String valor) {
        parametroRepository.save(new ParametroSistema(chave, valor));
    }

    private Sistema.TokenResumoResponse resumo(TokenIntegracao token) {
        return new Sistema.TokenResumoResponse(
                token.getId(), token.getNome(), token.getPrefixo(),
                token.getCriadoEm(), token.getUltimoUso(), token.isRevogado());
    }
}
