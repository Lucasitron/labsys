<?php

namespace App\Modules\Estoque\Services;

use App\Modules\Estoque\Enums\StatusEmprestimo;
use App\Modules\Estoque\Enums\TipoSaida;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Events\EmprestimoAtrasadoEvent;
use App\Modules\Estoque\Models\Emprestimo;
use App\Modules\Estoque\Models\Item;
use App\Modules\Estoque\Models\SaidaEstoque;
use App\Modules\Estoque\Policies\EstoquePolicy;
use App\Modules\Rh\Contracts\RhContract;
use App\Shared\Exceptions\ForbiddenException;
use App\Shared\Exceptions\ResourceNotFoundException;
use App\Shared\Exceptions\SaldoInsuficienteException;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

/** Empréstimos de equipamentos/ferramentas (regras só aqui). */
class EmprestimoService
{
    public function __construct(private ItemService $itens) {}

    public function criar(array $dados, EstoquePrincipal $principal): Emprestimo
    {
        EstoquePolicy::exigeEdicaoMovimentacao($principal);

        $idPessoa = (int) $dados['idPessoa'];
        if (! $principal->isAdmin() && $principal->idPessoa !== $idPessoa) {
            throw new ForbiddenException('Níveis não-admin só podem registrar empréstimos para si próprios');
        }

        if ($dados['dataDevolucaoPrevista'] < today()->toDateString()) {
            throw new \InvalidArgumentException('Data de devolução prevista não pode ser anterior à data de hoje');
        }

        return DB::transaction(function () use ($dados, $idPessoa) {
            $item = Item::whereKey((int) $dados['idItem'])->lockForUpdate()->first()
                ?? throw new ResourceNotFoundException("Item não encontrado: {$dados['idItem']}");

            $quantidade = number_format((float) $dados['quantidade'], 2, '.', '');
            $novoSaldo = (int) round((float) $item->quantidade_atual * 100)
                - (int) round((float) $quantidade * 100);

            if ($novoSaldo < 0) {
                throw new SaldoInsuficienteException(
                    "Estoque insuficiente para o item {$item->nome}: saldo atual {$item->quantidade_atual}, solicitado {$quantidade}",
                );
            }

            $item->quantidade_atual = number_format($novoSaldo / 100, 2, '.', '');
            $item->save();

            $emprestimo = Emprestimo::create([
                'id_item' => $item->getKey(),
                'id_pessoa' => $idPessoa,
                'quantidade' => $quantidade,
                'data_emprestimo' => today()->toDateString(),
                'data_devolucao_prevista' => $dados['dataDevolucaoPrevista'],
                'status' => StatusEmprestimo::ATIVO,
                'observacao' => $dados['observacao'] ?? null,
                'responsavel' => $dados['responsavel'] ?? null,
            ]);

            SaidaEstoque::create([
                'id_item' => $item->getKey(),
                'quantidade' => $quantidade,
                'tipo_saida' => TipoSaida::EMPRESTIMO,
                'id_referencia' => $emprestimo->getKey(),
                'data_saida' => now(),
                'observacao' => 'Empréstimo #'.$emprestimo->getKey(),
            ]);

            $this->itens->verificarEstoqueBaixo($item);

            return $emprestimo->refresh()->load('item');
        });
    }

    public function devolver(int $id, EstoquePrincipal $principal): Emprestimo
    {
        EstoquePolicy::exigeEdicaoMovimentacao($principal);

        return DB::transaction(function () use ($id, $principal) {
            $emprestimo = Emprestimo::find($id)
                ?? throw new ResourceNotFoundException("Empréstimo não encontrado: {$id}");

            if (! $principal->isAdmin() && (int) $emprestimo->id_pessoa !== $principal->idPessoa) {
                throw new ForbiddenException('Apenas o Admin ou o responsável pelo empréstimo pode registrar a devolução');
            }

            if ($emprestimo->status === StatusEmprestimo::DEVOLVIDO) {
                throw new \InvalidArgumentException('Empréstimo já devolvido');
            }

            $item = Item::whereKey($emprestimo->id_item)->lockForUpdate()->first()
                ?? throw new ResourceNotFoundException("Item não encontrado: {$emprestimo->id_item}");

            $item->quantidade_atual = number_format(
                ((int) round((float) $item->quantidade_atual * 100)
                    + (int) round((float) $emprestimo->quantidade * 100)) / 100,
                2, '.', '',
            );
            $item->save();

            $emprestimo->data_devolucao_real = today()->toDateString();
            $emprestimo->status = StatusEmprestimo::DEVOLVIDO;
            $emprestimo->save();

            return $emprestimo->refresh()->load('item');
        });
    }

    /**
     * @return list<Emprestimo>
     * nulo/vazio = todos; `ativos` = ATIVO+ATRASADO; `atrasados` = vencidos;
     * `historico` = DEVOLVIDO. Escopo E-7 server-side.
     */
    public function listar(?string $status, EstoquePrincipal $principal): array
    {
        EstoquePolicy::exigeLeitura($principal);

        $query = match (true) {
            $status === null || trim($status) === '' => Emprestimo::query(),
            strcasecmp(trim($status), 'ativos') === 0 => Emprestimo::whereIn(
                'status', [StatusEmprestimo::ATIVO, StatusEmprestimo::ATRASADO],
            ),
            strcasecmp(trim($status), 'atrasados') === 0 => $this->vencidos(),
            strcasecmp(trim($status), 'historico') === 0 => Emprestimo::where(
                'status', StatusEmprestimo::DEVOLVIDO,
            ),
            default => throw new \InvalidArgumentException('Status inválido. Use ativos, atrasados ou historico'),
        };

        $emprestimos = $query->with('item')->orderBy('id_emprestimo')->get()->all();

        return $this->escopo($emprestimos, $principal);
    }

    /** @return list<Emprestimo> */
    public function listarAtrasados(EstoquePrincipal $principal): array
    {
        EstoquePolicy::exigeLeitura($principal);

        return $this->escopo(
            $this->vencidos()->with('item')->orderBy('id_emprestimo')->get()->all(),
            $principal,
        );
    }

    public function buscar(int $id, EstoquePrincipal $principal): Emprestimo
    {
        EstoquePolicy::exigeLeitura($principal);

        $emprestimo = Emprestimo::with('item')->find($id)
            ?? throw new ResourceNotFoundException("Empréstimo não encontrado: {$id}");

        EstoquePolicy::exigeAcessoEmprestimo($principal, (int) $emprestimo->id_pessoa);

        return $emprestimo;
    }

    /**
     * Marca ATIVO+vencido como ATRASADO + 1 evento por marcado.
     * Executado manualmente e pelo scheduler 03:00 (E10).
     */
    public function verificarAtrasados(): int
    {
        $atrasados = Emprestimo::where('status', StatusEmprestimo::ATIVO)
            ->whereDate('data_devolucao_prevista', '<', today()->toDateString())
            ->get();

        foreach ($atrasados as $emprestimo) {
            $emprestimo->status = StatusEmprestimo::ATRASADO;
            $emprestimo->save();

            try {
                event(new EmprestimoAtrasadoEvent(
                    (int) $emprestimo->getKey(),
                    (int) $emprestimo->id_pessoa,
                    (int) $emprestimo->id_item,
                    substr((string) $emprestimo->data_devolucao_prevista, 0, 10),
                ));
            } catch (\Throwable $e) {
                Log::warning("Falha ao publicar emprestimo.atrasado.event do empréstimo {$emprestimo->getKey()}: {$e->getMessage()}");
            }
        }

        Log::info('Marcados '.count($atrasados).' empréstimo(s) como atrasado(s)');

        return count($atrasados);
    }

    /**
     * Rótulo do tomador (E-4): nome oficial do RH, fail-soft `Pessoa #id`
     * quando o RH não responde — nunca 500 por causa do nome.
     */
    public function tomadorPara(Emprestimo $emprestimo): string
    {
        try {
            $nome = app(RhContract::class)->nomePessoa((int) $emprestimo->id_pessoa);
        } catch (\Throwable) {
            $nome = null;
        }

        return $nome ?? "Pessoa #{$emprestimo->id_pessoa}";
    }

    private function vencidos()
    {
        return Emprestimo::whereIn('status', [StatusEmprestimo::ATIVO, StatusEmprestimo::ATRASADO])
            ->whereDate('data_devolucao_prevista', '<', today()->toDateString());
    }

    /** E-7: Admin tudo; demais, só `id_pessoa` próprio. */
    private function escopo(array $emprestimos, EstoquePrincipal $principal): array
    {
        if ($principal->isAdmin()) {
            return $emprestimos;
        }

        return array_values(array_filter(
            $emprestimos,
            fn (Emprestimo $e) => (int) $e->id_pessoa === $principal->idPessoa,
        ));
    }
}
