package com.fablab.auth.service;

import com.fablab.auth.dto.Sistema;
import com.fablab.auth.entity.TokenIntegracao;
import com.fablab.auth.exception.ConfiguracaoInvalidaException;
import com.fablab.auth.exception.ResourceNotFoundException;
import com.fablab.auth.repository.TokenIntegracaoRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tokens de integração (C-5): chave exibida 1× na criação, armazenado
 * SOMENTE hash SHA-256 + prefixo. NUNCA logar/expor a chave após criar.
 */
@Service
public class TokenIntegracaoService {

    private static final String PREFIXO_FAMILIA = "fl_live_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenIntegracaoRepository repository;

    public TokenIntegracaoService(TokenIntegracaoRepository repository) {
        this.repository = repository;
    }

    /**
     * Lista ativa sem segredo (sem chave, sem hash).
     */
    @Transactional(readOnly = true)
    public List<Sistema.TokenResumoResponse> listarAtivos() {
        return repository.findByRevogadoFalseOrderByCriadoEmDesc()
                .stream().map(this::resumo).toList();
    }

    /**
     * Gera chave exibida UMA vez; persiste só hash + prefixo.
     */
    @Transactional
    public Sistema.TokenCriadoResponse criar(String nomeRaw) {
        String nome = nomeRaw == null ? "" : nomeRaw.trim();
        if (nome.isEmpty()) {
            throw new ConfiguracaoInvalidaException("Nome da integração é obrigatório");
        }
        if (nome.length() > 128) {
            throw new ConfiguracaoInvalidaException("Nome da integração deve ter no máximo 128 caracteres");
        }
        if (repository.existsByNomeAndRevogadoFalse(nome)) {
            throw new ConfiguracaoInvalidaException("Já existe um token ativo com este nome");
        }
        byte[] aleatorio = new byte[24];
        RANDOM.nextBytes(aleatorio);
        String chave = PREFIXO_FAMILIA + HexFormat.of().formatHex(aleatorio);
        String prefixo = chave.substring(0, 12);
        TokenIntegracao token = new TokenIntegracao();
        token.setNome(nome);
        token.setPrefixo(prefixo);
        token.setHash(sha256Hex(chave));
        token.setCriadoEm(Instant.now());
        token.setRevogado(false);
        token = repository.save(token);
        return new Sistema.TokenCriadoResponse(
                token.getId(), token.getNome(), token.getPrefixo(), token.getCriadoEm(), chave);
    }

    /**
     * Revoga (soft). 404 se inexistente ou já revogado.
     */
    @Transactional
    public void revogar(Long id) {
        TokenIntegracao token = repository.findById(id)
                .filter(t -> !t.isRevogado())
                .orElseThrow(() -> new ResourceNotFoundException("Token de integração não encontrado"));
        token.setRevogado(true);
        repository.save(token);
    }

    /**
     * Registra uso da chave (ponto de integração futuro; sem chamadores
     * hoje — follow-up reportado).
     */
    @Transactional
    public void registrarUsoPorHash(String hash) {
        repository.findByHash(hash).filter(t -> !t.isRevogado()).ifPresent(token -> {
            token.setUltimoUso(Instant.now());
            repository.save(token);
        });
    }

    static String sha256Hex(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    private Sistema.TokenResumoResponse resumo(TokenIntegracao token) {
        return new Sistema.TokenResumoResponse(
                token.getId(), token.getNome(), token.getPrefixo(),
                token.getCriadoEm(), token.getUltimoUso(), token.isRevogado());
    }
}
