<?php

namespace App\Modules\Rh\Services;

use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\RhPrincipal;

/**
 * Níveis (F7): matriz de capacidades (espelho do enforcement) + convites.
 * Alteração de membro delega ao FuncionarioService (histórico + evento únicos).
 */
class NivelService
{
    public function __construct(private FuncionarioService $funcionarios) {}

    /** Matriz nível × capacidade aplicada no servidor. */
    public function matriz(): array
    {
        $contagem = Funcionario::selectRaw('nivel_acesso, COUNT(*) as total')
            ->groupBy('nivel_acesso')
            ->pluck('total', 'nivel_acesso')
            ->all();

        $niveis = [];
        foreach (NivelAcesso::cases() as $nivel) {
            $niveis[] = [
                'id' => strtolower($nivel->name),
                'codigo' => $nivel->value,
                'label' => $nivel->label(),
                'total' => (int) ($contagem[$nivel->value] ?? 0),
            ];
        }

        // TOTAL = tudo · PROPRIO = só o próprio · NAO = sem acesso.
        $permissao = fn (string $id, string $rotulo, string $admin, string $bolsista, string $voluntario, string $estagiario, string $recrutando) => [
            'id' => $id, 'rotulo' => $rotulo, 'admin' => $admin, 'bolsista' => $bolsista,
            'voluntario' => $voluntario, 'estagiario' => $estagiario, 'recrutando' => $recrutando,
        ];

        return [
            'niveis' => $niveis,
            'permissoes' => [
                $permissao('pessoas', 'Pessoas', 'TOTAL', 'TOTAL', 'TOTAL', 'PROPRIO', 'NAO'),
                $permissao('funcionarios', 'Funcionários e horas', 'TOTAL', 'PROPRIO', 'PROPRIO', 'PROPRIO', 'NAO'),
                $permissao('alterar-nivel', 'Alterar nível', 'TOTAL', 'NAO', 'NAO', 'NAO', 'NAO'),
                $permissao('apontamentos', 'Registrar horas', 'TOTAL', 'PROPRIO', 'PROPRIO', 'NAO', 'NAO'),
                $permissao('validar-horas', 'Validar horas', 'TOTAL', 'NAO', 'NAO', 'NAO', 'NAO'),
                $permissao('processo-seletivo', 'Processo seletivo', 'TOTAL', 'PROPRIO', 'PROPRIO', 'NAO', 'NAO'),
                $permissao('treinamentos', 'Treinamentos', 'TOTAL', 'PROPRIO', 'PROPRIO', 'NAO', 'NAO'),
                $permissao('certificados', 'Certificados', 'TOTAL', 'PROPRIO', 'PROPRIO', 'PROPRIO', 'NAO'),
                $permissao('decidir-certificados', 'Aprovar certificados', 'TOTAL', 'NAO', 'NAO', 'NAO', 'NAO'),
            ],
        ];
    }

    public function alterarMembro(int $idFuncionario, NivelAcesso $nivel, RhPrincipal $principal): array
    {
        return $this->funcionarios->alterarNivel($idFuncionario, $nivel, $principal);
    }

    /** Convite: pessoa (+funcionário); nível 4 nasce Recrutando. */
    public function convidar(array $dados): Funcionario
    {
        $nivel = $dados['nivelAcesso'] ?? NivelAcesso::BOLSISTA;

        $pessoa = app(PessoaService::class)->cadastrar([
            'nome_completo' => $dados['nomeCompleto'],
            'matricula' => $dados['matricula'],
            'contato' => $dados['contato'] ?? null,
            'status' => $nivel === NivelAcesso::RECRUTANDO ? PessoaStatus::RECRUTANDO : PessoaStatus::ATIVO,
        ]);

        return $this->funcionarios->vincular([
            'id_pessoa' => (int) $pessoa->getKey(),
            'nivel_acesso' => $nivel,
            'departamento' => $dados['departamento'] ?? null,
        ]);
    }
}
