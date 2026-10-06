<?php

namespace App\Modules\Rh\Services;

use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\AvaliacaoTreinamento;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\HistoricoNivel;
use App\Modules\Rh\Models\Pessoa;
use App\Modules\Rh\Models\ProcessoSeletivo;
use App\Modules\Rh\Policies\RhPolicy;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Rules\CpfRule;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

/**
 * Pessoas (F1): CRUD + matrícula única + CPF opcional/único/mascarado + busca/facetas.
 * Regra só aqui — controllers apenas delegam.
 */
class PessoaService
{
    private const PAGE_SIZE_MAX = 100;

    public function cadastrar(array $dados): Pessoa
    {
        $this->exigeMatriculaLivre($dados['matricula']);
        $dados['cpf'] = $this->normalizarCpf($dados['cpf'] ?? null, null);
        $dados['status'] ??= PessoaStatus::ATIVO;

        return Pessoa::create($dados);
    }

    /** Pagina + busca LIKE + filtros + facetas. Estagiário vê só o próprio. */
    public function listar(
        ?string $busca,
        ?string $departamento,
        ?NivelAcesso $nivel,
        ?PessoaStatus $status,
        int $page,
        int $pageSize,
        RhPrincipal $principal,
    ): LengthAwarePaginator {
        RhPolicy::exigeModuloPessoas($principal);

        $busca = trim((string) $busca) === '' ? '' : trim((string) $busca);
        $departamento = trim((string) $departamento) === '' ? null : trim((string) $departamento);
        $page = max($page, 1);
        $pageSize = min(max($pageSize, 1), self::PAGE_SIZE_MAX);

        $query = Pessoa::query()
            ->when($busca !== '', fn ($q) => $q->where(
                fn ($w) => $w->where('nome_completo', 'ilike', "%{$busca}%")
                    ->orWhere('matricula', 'ilike', "%{$busca}%")
                    ->orWhere('contato', 'ilike', "%{$busca}%")
            ))
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->when($nivel !== null || $departamento !== null, fn ($q) => $q->whereHas(
                'funcionario',
                fn ($f) => $f
                    ->when($nivel !== null, fn ($w) => $w->where('nivel_acesso', $nivel))
                    ->when($departamento !== null, fn ($w) => $w->where('departamento', 'ilike', $departamento))
            ))
            ->orderBy('id');

        // Estagiário (e demais sem visão global): só o próprio registro.
        if (! RhPolicy::podeVerTodos($principal)) {
            $query->where('id', $principal->idPessoa);
        }

        return $query->paginate($pageSize, ['*'], 'page', $page);
    }

    /** Contagens p/ facetas (status com a busca aplicada; nível/setor globais). */
    public function facetas(?string $busca): array
    {
        $busca = trim((string) $busca);

        $porStatus = Pessoa::query()
            ->when($busca !== '', fn ($q) => $q->where(
                fn ($w) => $w->where('nome_completo', 'ilike', "%{$busca}%")
                    ->orWhere('matricula', 'ilike', "%{$busca}%")
                    ->orWhere('contato', 'ilike', "%{$busca}%")
            ))
            ->selectRaw('status, COUNT(*) as total')
            ->groupBy('status')
            ->pluck('total', 'status')
            ->all();

        $statuses = [];
        foreach ($porStatus as $code => $total) {
            $enum = PessoaStatus::from((int) $code);
            $statuses[] = ['id' => strtolower($enum->name), 'label' => $enum->label(), 'count' => (int) $total];
        }

        $niveis = [];
        foreach (
            Funcionario::selectRaw('nivel_acesso, COUNT(*) as total')->groupBy('nivel_acesso')->pluck('total', 'nivel_acesso') as $code => $total
        ) {
            $enum = NivelAcesso::from((int) $code);
            $niveis[] = ['id' => strtolower($enum->name), 'label' => $enum->label(), 'count' => (int) $total];
        }

        $setores = [];
        foreach (
            Funcionario::selectRaw('departamento, COUNT(*) as total')->whereNotNull('departamento')->groupBy('departamento')->pluck('total', 'departamento') as $dep => $total
        ) {
            $setores[] = ['id' => $dep, 'label' => $dep, 'count' => (int) $total];
        }

        return ['status' => $statuses, 'niveis' => $niveis, 'setores' => $setores];
    }

    public function buscar(int $id, RhPrincipal $principal): Pessoa
    {
        $pessoa = $this->obter($id);
        RhPolicy::exigeAcessoAPessoa($principal, (int) $pessoa->getKey());

        return $pessoa;
    }

    public function atualizar(int $id, array $dados, RhPrincipal $principal): Pessoa
    {
        $pessoa = $this->obter($id);
        RhPolicy::exigeAcessoAPessoa($principal, (int) $pessoa->getKey());

        if (isset($dados['matricula'])) {
            $this->exigeMatriculaLivre($dados['matricula'], $id);
        }

        if (array_key_exists('cpf', $dados)) {
            $dados['cpf'] = $this->normalizarCpf($dados['cpf'], $id);
        }

        $pessoa->fill($dados);
        $pessoa->save();

        return $pessoa->refresh();
    }

    /** Detalhe agregado (abas). Sem vínculo, blocos agregados degradam p/ vazio. */
    public function detalhe(int $id, RhPrincipal $principal): array
    {
        $pessoa = $this->buscar($id, $principal);
        $funcionario = Funcionario::where('id_pessoa', $pessoa->getKey())->first();

        if ($funcionario === null) {
            return [
                'pessoa' => $pessoa, 'idFuncionario' => null, 'nivel' => null, 'departamento' => null,
                'horasMes' => ['atual' => '0.00', 'anterior' => '0.00'],
                'treinamentos' => ['total' => 0, 'media' => null],
                'pendencias' => ['total' => 0],
                'horasPorStatus' => ['pendente' => 0, 'validado' => 0, 'rejeitado' => 0],
                'avaliacoes' => [], 'historicoNivel' => [],
            ];
        }

        $idFunc = (int) $funcionario->getKey();
        $hoje = today();
        $mesAnterior = $hoje->copy()->subMonthNoOverflow();

        $avaliacoes = AvaliacaoTreinamento::where('id_funcionario', $idFunc)->get();
        $media = $avaliacoes->isEmpty() ? null : round((float) $avaliacoes->avg('nota'), 2);

        $historico = HistoricoNivel::where('id_funcionario', $idFunc)
            ->with('admin.pessoa')
            ->orderBy('data_alteracao')
            ->get()
            ->map(fn (HistoricoNivel $h) => [
                'nivelAntigo' => $h->nivel_antigo->value,
                'nivelAntigoLabel' => $h->nivel_antigo->label(),
                'nivelNovo' => $h->nivel_novo->value,
                'nivelNovoLabel' => $h->nivel_novo->label(),
                'alteradoPor' => $h->admin->pessoa->nome_completo,
                'dataAlteracao' => $h->data_alteracao->toIso8601String(),
            ])->all();

        return [
            'pessoa' => $pessoa,
            'idFuncionario' => $idFunc,
            'nivel' => $funcionario->nivel_acesso->value,
            'departamento' => $funcionario->departamento,
            'horasMes' => [
                'atual' => $this->somaValidadas($idFunc, $hoje->copy()->startOfMonth(), $hoje),
                'anterior' => $this->somaValidadas($idFunc, $mesAnterior->copy()->startOfMonth(), $mesAnterior->copy()->endOfMonth()),
            ],
            'treinamentos' => ['total' => $avaliacoes->count(), 'media' => $media],
            'pendencias' => ['total' => $this->contarPorStatus($idFunc, StatusApontamento::PENDENTE)],
            'horasPorStatus' => [
                'pendente' => $this->contarPorStatus($idFunc, StatusApontamento::PENDENTE),
                'validado' => $this->contarPorStatus($idFunc, StatusApontamento::VALIDADO),
                'rejeitado' => $this->contarPorStatus($idFunc, StatusApontamento::REJEITADO),
            ],
            'avaliacoes' => $avaliacoes,
            'historicoNivel' => $historico,
        ];
    }

    /** Admin. Recusa com vínculo de funcionário ou processo (integridade). */
    public function excluir(int $id): void
    {
        $pessoa = $this->obter($id);

        if (Funcionario::where('id_pessoa', $id)->exists()) {
            throw new \InvalidArgumentException("Pessoa possui vínculo de funcionário e não pode ser excluída: {$id}");
        }

        if (ProcessoSeletivo::where('id_candidato', $id)->exists()) {
            throw new \InvalidArgumentException("Pessoa possui processo seletivo e não pode ser excluída: {$id}");
        }

        $pessoa->delete();
    }

    public function obter(int $id): Pessoa
    {
        return Pessoa::find($id)
            ?? throw new ResourceNotFoundException("Pessoa não encontrada: {$id}");
    }

    private function exigeMatriculaLivre(string $matricula, ?int $excetoId = null): void
    {
        $existe = Pessoa::where('matricula', $matricula)
            ->when($excetoId !== null, fn ($q) => $q->where('id', '!=', $excetoId))
            ->exists();

        if ($existe) {
            throw new \InvalidArgumentException("Matrícula já cadastrada: {$matricula}");
        }
    }

    /** Normaliza p/ dígitos; rejeita inválido ou duplicado (nulo quando ausente). */
    private function normalizarCpf(?string $cpf, ?int $excetoId): ?string
    {
        $normalizado = CpfRule::normalizar($cpf);

        if ($normalizado === null) {
            return null;
        }

        if (! CpfRule::valido($normalizado)) {
            throw new \InvalidArgumentException('CPF inválido');
        }

        $duplicado = Pessoa::where('cpf', $normalizado)
            ->when($excetoId !== null, fn ($q) => $q->where('id', '!=', $excetoId))
            ->exists();

        if ($duplicado) {
            throw new \InvalidArgumentException('CPF já cadastrado');
        }

        return $normalizado;
    }

    private function somaValidadas(int $idFuncionario, mixed $inicio, mixed $fim): string
    {
        $total = ApontamentoHoras::where('id_funcionario', $idFuncionario)
            ->where('status', StatusApontamento::VALIDADO)
            ->whereBetween('data', [$inicio->toDateString(), $fim->toDateString()])
            ->sum('horas_trabalhadas');

        return number_format((float) $total, 2, '.', '');
    }

    private function contarPorStatus(int $idFuncionario, StatusApontamento $status): int
    {
        return ApontamentoHoras::where('id_funcionario', $idFuncionario)
            ->where('status', $status)
            ->count();
    }
}
