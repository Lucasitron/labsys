package com.fablab.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Contratos de gestão de contas ({@code /api/usuarios}).
 *
 * <p>Opera apenas conta/nível/situação sobre {@code login} +
 * {@code user_permissions}. Identidade física (nome, telefone, foto) é do
 * {@code rh-service} e não é duplicada aqui.</p>
 */
public final class Usuarios {

    private Usuarios() {
    }

    public record CriarUsuarioRequest(

            @NotNull(message = "vínculo com pessoa (idUser) é obrigatório")
            Long idUser,

            @NotBlank(message = "e-mail é obrigatório")
            @Email(message = "e-mail inválido")
            @Size(max = 255, message = "e-mail deve ter no máximo 255 caracteres")
            String email,

            @NotBlank(message = "nome de usuário é obrigatório")
            @Size(max = 255, message = "nome de usuário deve ter no máximo 255 caracteres")
            String nomeUsuario,

            @NotBlank(message = "senha é obrigatória")
            @Size(min = 8, max = 72, message = "senha deve ter ao menos 8 caracteres")
            String senha,

            @Size(max = 255, message = "setor deve ter no máximo 255 caracteres")
            String setor,

            /** Código do nível RBAC (0-4). Ausente = Recrutando (4). */
            Integer nivel,

            /** Situação inicial (Ativo/Pendente/Desativado). Ausente = Pendente. */
            String situacao,

            /** Cartão RFID (uuid). Ausente = gerado automaticamente. */
            String uuid) {
    }

    public record AtualizarUsuarioRequest(

            @Email(message = "e-mail inválido")
            @Size(max = 255, message = "e-mail deve ter no máximo 255 caracteres")
            String email,

            @Size(max = 255, message = "nome de usuário deve ter no máximo 255 caracteres")
            String nomeUsuario,

            @Size(max = 255, message = "setor deve ter no máximo 255 caracteres")
            String setor,

            /** Código do nível RBAC (0-4). */
            Integer nivel,

            /** Situação (Ativo/Pendente/Desativado). */
            String situacao) {
    }

    public record AlterarStatusRequest(

            @NotBlank(message = "situação é obrigatória")
            String situacao) {
    }

    public record UsuarioResponse(
            Long id,
            Long idUser,
            String email,
            String nomeUsuario,
            String setor,
            String nivel,
            Integer nivelCodigo,
            String situacao) {
    }

    public record Contagens(
            long total,
            long ativos,
            long pendentes,
            long desativados,
            Map<String, Long> porNivel) {
    }

    public record PaginaUsuariosResponse(
            List<UsuarioResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            Contagens counts) {
    }
}
