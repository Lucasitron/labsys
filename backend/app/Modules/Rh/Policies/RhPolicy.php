<?php

namespace App\Modules\Rh\Policies;

use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\ProcessoSeletivo;
use App\Modules\Rh\Models\Tutor;
use App\Modules\Rh\RhPrincipal;
use App\Shared\Exceptions\ForbiddenException;

/**
 * Autorização do RH (port de AutorizacaoHelper + @PreAuthorize).
 * Gate `admin` (rota) cobre Admin-only; aqui os vínculos (próprio/tutor).
 */
class RhPolicy
{
    /** Admin ou dono do vínculo. */
    public static function ehAdminOuProprio(RhPrincipal $principal, Funcionario $funcionario): bool
    {
        return $principal->isAdmin()
            || ($principal->idFuncionario !== null
                && $principal->idFuncionario === (int) $funcionario->getKey());
    }

    /**
     * Exige vínculo com funcionário (próprio-registro) — sem vínculo, só Admin passa.
     *
     * @throws ForbiddenException
     */
    public static function exigeVinculoOuAdmin(RhPrincipal $principal): int
    {
        if ($principal->isAdmin() && $principal->idFuncionario === null) {
            // Admin sem vínculo: age no escopo geral (leituras/decisões por id).
            // Escritas "próprias" (solicitar/registrar) falham no service por falta de eu.
            return 0;
        }

        if ($principal->idFuncionario === null) {
            throw new ForbiddenException('Operação restrita a usuários vinculados a um funcionário');
        }

        return $principal->idFuncionario;
    }

    /** Recrutando não acessa o módulo de pessoas. */
    public static function exigeModuloPessoas(RhPrincipal $principal): void
    {
        if ($principal->nivel === NivelAcesso::RECRUTANDO) {
            throw new ForbiddenException('Acesso negado ao módulo de pessoas');
        }
    }

    /** Admin, Bolsista e Voluntário veem todos; demais, só o próprio registro. */
    public static function podeVerTodos(RhPrincipal $principal): bool
    {
        return $principal->isAdmin()
            || $principal->nivel === NivelAcesso::BOLSISTA
            || $principal->nivel === NivelAcesso::VOLUNTARIO;
    }

    /** @throws ForbiddenException */
    public static function exigeAcessoAPessoa(RhPrincipal $principal, int $idPessoa): void
    {
        if (self::podeVerTodos($principal)) {
            return;
        }

        if ($principal->idPessoa !== $idPessoa) {
            throw new ForbiddenException('Acesso apenas aos seus próprios dados');
        }
    }

    public static function ehTutor(RhPrincipal $principal): bool
    {
        return $principal->idFuncionario !== null
            && Tutor::where('id_funcionario', $principal->idFuncionario)->exists();
    }

    /** Admin ou tutor (do processo, quando informado). */
    public static function exigeTutorOuAdmin(RhPrincipal $principal, ?ProcessoSeletivo $processo = null): void
    {
        if ($principal->isAdmin()) {
            return;
        }

        if (! self::ehTutor($principal)) {
            throw new ForbiddenException('Apenas Admin ou tutor podem gerir o processo seletivo');
        }

        if ($processo !== null && (int) $processo->id_tutor !== $principal->idFuncionario) {
            throw new ForbiddenException('Apenas o tutor responsável pode alterar este processo');
        }
    }

    /** Validação de horas: Admin ou tutor responsável. */
    public static function podeValidarHoras(RhPrincipal $principal): bool
    {
        return $principal->isAdmin() || self::ehTutor($principal);
    }
}
