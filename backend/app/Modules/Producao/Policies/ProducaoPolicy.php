<?php

namespace App\Modules\Producao\Policies;

use App\Modules\Auth\Enums\Role;
use App\Modules\Producao\Models\SetorResponsavel;
use App\Modules\Producao\ProducaoPrincipal;
use App\Shared\Exceptions\ForbiddenException;

/**
 * Autorização do Producao (rbac-matrix.md ∩ requirements §7 prevalecem sobre o
 * Java — plan §M6.1.4).
 *
 * - Leitura: qualquer autenticado, exceto Recrutando (matriz: `—` no módulo).
 * - Escrita por vínculo (`isResponsavelAtribuido`: Admin sempre; demais só se
 *   `id == claim id_user`): projetos/tarefas (responsável do projeto),
 *   kanban mover/remover (responsável do cartão), inspeção (inspetor),
 *   projetos-mesa (dono), `PATCH` tarefa (responsável da tarefa).
 * - Admin-only (deltas): cadastro/status de máquina, designação de
 *   responsáveis de setor, advertências, auditorias, parâmetros, `DELETE`
 *   inspeção. Inclusão no Kanban = Admin+Bolsista (Voluntário `—` no §7).
 * - Setor/checklist/materiais/sinalizações: Admin ou B/V com vínculo de
 *   responsável ativo do setor (aprovação = ação do Admin, sem workflow novo).
 */
class ProducaoPolicy
{
    /** @throws ForbiddenException */
    public static function exigeLeitura(ProducaoPrincipal $principal): void
    {
        if ($principal->role === Role::RECRUTANDO) {
            throw new ForbiddenException('Acesso negado ao módulo de produção');
        }
    }

    /** Admin sempre; demais só se forem o responsável/atribuído. */
    public static function isResponsavelAtribuido(?int $idResponsavel, ProducaoPrincipal $principal): bool
    {
        if ($principal->isAdmin()) {
            return true;
        }

        return $idResponsavel !== null && $idResponsavel === $principal->idPessoa;
    }

    /** @throws ForbiddenException */
    public static function exigeResponsavel(?int $idResponsavel, ProducaoPrincipal $principal): void
    {
        self::exigeLeitura($principal);

        if (! self::isResponsavelAtribuido($idResponsavel, $principal)) {
            throw new ForbiddenException('Apenas o responsável atribuído ou Admin pode alterar este recurso');
        }
    }

    /** @throws ForbiddenException */
    public static function exigeAdmin(ProducaoPrincipal $principal): void
    {
        self::exigeLeitura($principal);

        if (! $principal->isAdmin()) {
            throw new ForbiddenException('Apenas Admin pode executar esta ação');
        }
    }

    /** Inclusão no Kanban: Admin+Bolsista (requirements §7; Voluntário `—`). */
    public static function exigeIncluirKanban(ProducaoPrincipal $principal): void
    {
        self::exigeLeitura($principal);

        if (! $principal->isAdmin() && $principal->role !== Role::BOLSISTA) {
            throw new ForbiddenException('Apenas Admin ou Bolsista pode incluir encomenda no Kanban');
        }
    }

    /** Uso de máquina: B/V/E (+Admin); Recrutando fora (leitura geral). */
    public static function exigeUsoMaquina(ProducaoPrincipal $principal): void
    {
        self::exigeLeitura($principal);
    }

    /**
     * Criação de setor: Admin ou Bolsista/Voluntário com vínculo de
     * responsável ativo em algum setor (Estagiário só lê).
     *
     * @throws ForbiddenException
     */
    public static function exigeCriarSetor(ProducaoPrincipal $principal): void
    {
        self::exigeLeitura($principal);

        if ($principal->isAdmin()) {
            return;
        }

        if (! in_array($principal->role, [Role::BOLSISTA, Role::VOLUNTARIO], true)) {
            throw new ForbiddenException('Apenas Admin ou responsável de setor pode criar setores');
        }

        $vinculado = SetorResponsavel::where('id_funcionario', $principal->idPessoa)
            ->where('ativo', true)
            ->exists();

        if (! $vinculado) {
            throw new ForbiddenException('Apenas Admin ou responsável de setor pode criar setores');
        }
    }

    /**
     * Escrita de setor: Admin ou Bolsista/Voluntário com vínculo de
     * responsável ativo do setor (Estagiário só lê).
     *
     * @throws ForbiddenException
     */
    public static function exigeEscritaSetor(int $idSetor, ProducaoPrincipal $principal): void
    {
        self::exigeLeitura($principal);

        if ($principal->isAdmin()) {
            return;
        }

        if (! in_array($principal->role, [Role::BOLSISTA, Role::VOLUNTARIO], true)) {
            throw new ForbiddenException('Apenas Admin ou responsável do setor pode alterar este recurso');
        }

        $vinculado = SetorResponsavel::where('id_setor', $idSetor)
            ->where('id_funcionario', $principal->idPessoa)
            ->where('ativo', true)
            ->exists();

        if (! $vinculado) {
            throw new ForbiddenException('Apenas Admin ou responsável do setor pode alterar este recurso');
        }
    }
}
