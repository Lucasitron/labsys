package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fablab.auth.dto.RbacResponse;
import com.fablab.auth.entity.Role;
import com.fablab.auth.service.RbacService;
import org.junit.jupiter.api.Test;

class RbacServiceTest {

    private final RbacService rbacService = new RbacService();

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
}