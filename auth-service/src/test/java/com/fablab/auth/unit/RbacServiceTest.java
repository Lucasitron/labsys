package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fablab.auth.dto.Permissoes;
import com.fablab.auth.dto.RbacResponse;
import com.fablab.auth.entity.Modulo;
import com.fablab.auth.entity.PermissaoMatriz;
import com.fablab.auth.entity.PermissaoMatrizId;
import com.fablab.auth.entity.PermissaoNivel;
import com.fablab.auth.entity.Role;
import com.fablab.auth.exception.PermissaoInvalidaException;
import com.fablab.auth.repository.PermissaoMatrizRepository;
import com.fablab.auth.service.RbacService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RbacServiceTest {

    @Mock
    private PermissaoMatrizRepository repository;

    private RbacService rbacService;

    /** Simula a tabela permissao_matriz em memória. */
    private final Map<PermissaoMatrizId, PermissaoMatriz> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        store.clear();
        when(repository.count()).thenAnswer(invocation -> (long) store.size());
        when(repository.findAll()).thenAnswer(invocation -> new ArrayList<>(store.values()));
        when(repository.findById(any(PermissaoMatrizId.class)))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));
        when(repository.save(any(PermissaoMatriz.class))).thenAnswer(invocation -> {
            PermissaoMatriz entity = invocation.getArgument(0);
            store.put(new PermissaoMatrizId(entity.getModulo(), entity.getRole()), entity);
            return entity;
        });
        when(repository.saveAll(Mockito.<List<PermissaoMatriz>>any())).thenAnswer(invocation -> {
            List<PermissaoMatriz> entities = invocation.getArgument(0);
            entities.forEach(entity ->
                    store.put(new PermissaoMatrizId(entity.getModulo(), entity.getRole()), entity));
            return entities;
        });
        rbacService = new RbacService(repository);
    }

    @Test
    void adminHasWildcardPermission() {
        RbacResponse response = rbacService.getMatrix(Role.ADMIN);
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.code()).isEqualTo(0);
        assertThat(response.permissions()).containsExactly("*");
    }

    @Test
    void bolsistaHasWritePermissions() {
        RbacResponse response = rbacService.getMatrix(Role.BOLSISTA);
        assertThat(response.code()).isEqualTo(1);
        assertThat(response.permissions())
                .contains("projetos:write", "ponto:write", "relatorios:export");
    }

    @Test
    void recrutandoHasOnlyReadAccess() {
        RbacResponse response = rbacService.getMatrix(Role.RECRUTANDO);
        assertThat(response.label()).isEqualTo("Recrutando");
        assertThat(response.permissions()).containsExactly("catalogo:read");
    }

    @Test
    void voluntarioMatrixMatchesDocumentation() {
        RbacResponse response = rbacService.getMatrix(Role.VOLUNTARIO);
        assertThat(response.code()).isEqualTo(2);
        assertThat(response.permissions()).containsExactlyInAnyOrder("catalogo:read", "equipamentos:read");
    }

    @Test
    void tabelaVaziaRecebeSeedPadraoNaPrimeiraLeitura() {
        Permissoes.MatrizPermissoesResponse response = rbacService.getFullMatrix();

        assertThat(response.matriz()).hasSize(Modulo.values().length * Role.values().length);
        assertThat(celula(response, "estoque", "ADMIN")).isEqualTo("Editar");
        assertThat(celula(response, "estoque", "BOLSISTA")).isEqualTo("Ver");
        assertThat(celula(response, "rh", "RECRUTANDO")).isEqualTo("Nenhum");
    }

    @Test
    void celulaAtualizadaPersisteParaProximaLeitura() {
        Permissoes.CelulaPermissao atualizada =
                rbacService.updateCell("estoque", "BOLSISTA", "Editar");

        assertThat(atualizada.valor()).isEqualTo("Editar");

        Permissoes.MatrizPermissoesResponse response = rbacService.getFullMatrix();
        assertThat(celula(response, "estoque", "BOLSISTA")).isEqualTo("Editar");
    }

    @Test
    void novaInstanciaLeCelulaPersistidaSimulandoRestart() {
        rbacService.updateCell("rh", "VOLUNTARIO", "Nenhum");

        RbacService reiniciado = new RbacService(repository);
        Permissoes.MatrizPermissoesResponse response = reiniciado.getFullMatrix();

        assertThat(celula(response, "rh", "VOLUNTARIO")).isEqualTo("Nenhum");
    }

    @Test
    void atualizarCelulaInvalidaLancaErroPt() {
        assertThatThrownBy(() -> rbacService.updateCell("invalido", "ADMIN", "Ver"))
                .isInstanceOf(PermissaoInvalidaException.class);
        assertThatThrownBy(() -> rbacService.updateCell("rh", "9", "Ver"))
                .isInstanceOf(PermissaoInvalidaException.class);
        assertThatThrownBy(() -> rbacService.updateCell("rh", "ADMIN", "Apagar"))
                .isInstanceOf(PermissaoInvalidaException.class);
    }

    private static String celula(Permissoes.MatrizPermissoesResponse response, String modulo, String nivel) {
        return response.matriz().stream()
                .filter(celula -> celula.modulo().equals(modulo) && celula.nivel().equals(nivel))
                .map(Permissoes.CelulaPermissao::valor)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Célula ausente: " + modulo + "/" + nivel));
    }

    @Test
    void nivelPadraoSegueRegraAdminEditaRecrutandoNenhum() {
        assertThat(RbacService.defaultNivel(Role.ADMIN)).isEqualTo(PermissaoNivel.EDITAR);
        assertThat(RbacService.defaultNivel(Role.BOLSISTA)).isEqualTo(PermissaoNivel.VER);
        assertThat(RbacService.defaultNivel(Role.RECRUTANDO)).isEqualTo(PermissaoNivel.NENHUM);
    }
}
