<?php

namespace App\Modules\Vendas\Services;

use App\Modules\Vendas\Enums\StatusSolicitacao;
use App\Modules\Vendas\Enums\TipoAlvoSolicitacao;
use App\Modules\Vendas\Enums\TipoSolicitacao;
use App\Modules\Vendas\Models\SolicitacaoEdicao;
use App\Modules\Vendas\Policies\VendasPolicy;
use App\Modules\Vendas\VendasPrincipal;
use App\Shared\Exceptions\ConflitoException;
use App\Shared\Exceptions\ResourceNotFoundException;

/**
 * Solicitações de edição (contrato mínimo D-4): qualquer autenticado (exceto
 * Recrutando) solicita; só Admin decide; decidir só flipa status (+decidido
 * por/motivo/data) — sem aplicação automática no alvo.
 */
class SolicitacaoService
{
    public function solicitar(array $dados, VendasPrincipal $principal): SolicitacaoEdicao
    {
        VendasPolicy::exigeLeitura($principal);

        $tipo = TipoSolicitacao::tryFrom($dados['tipo'] ?? '')
            ?? throw new \InvalidArgumentException('Tipo inválido (ALTERACAO_DADOS, MUDANCA_STATUS, MOVER_ENCOMENDA ou OUTRA)');
        $alvoTipo = TipoAlvoSolicitacao::tryFrom($dados['alvoTipo'] ?? '')
            ?? throw new \InvalidArgumentException('Alvo inválido (CLIENTE, ORCAMENTO ou ENCOMENDA)');

        return SolicitacaoEdicao::create([
            'tipo' => $tipo,
            'alvo_tipo' => $alvoTipo,
            'alvo_id' => $dados['alvoId'],
            'campo' => $dados['campo'],
            'valor_atual' => $dados['valorAtual'] ?? null,
            'valor_proposto' => $dados['valorProposto'],
            'justificativa' => $dados['justificativa'],
            'status' => StatusSolicitacao::PENDENTE,
            'solicitante_id' => $principal->idPessoa,
            'data_criacao' => now()->toDateTimeString(),
        ]);
    }

    /** @return array{solicitacoes:list<SolicitacaoEdicao>,counts:array<string,int>} */
    public function listar(?StatusSolicitacao $status, VendasPrincipal $principal): array
    {
        VendasPolicy::exigeLeitura($principal);

        $solicitacoes = SolicitacaoEdicao::query()
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->orderByDesc('id_solicitacao')
            ->get();

        $counts = [];
        foreach (StatusSolicitacao::cases() as $caso) {
            $counts[$caso->value] = SolicitacaoEdicao::where('status', $caso)->count();
        }

        return ['solicitacoes' => $solicitacoes->all(), 'counts' => $counts];
    }

    /** Decidir (Admin-only): só flipa status — D-4, sem tocar no alvo. */
    public function decidir(int $id, string $decisao, ?string $motivo, VendasPrincipal $principal): SolicitacaoEdicao
    {
        VendasPolicy::exigeEscrita($principal);

        $solicitacao = SolicitacaoEdicao::find($id)
            ?? throw new ResourceNotFoundException("Solicitação não encontrada: {$id}");

        if ($solicitacao->status !== StatusSolicitacao::PENDENTE) {
            throw new ConflitoException('Solicitação já decidida');
        }

        $decisao = mb_strtoupper(trim($decisao));
        if ($decisao === 'APROVAR') {
            $solicitacao->status = StatusSolicitacao::APROVADA;
        } elseif ($decisao === 'REJEITAR') {
            if ($motivo === null || trim($motivo) === '') {
                throw new \InvalidArgumentException('O motivo é obrigatório ao rejeitar');
            }
            $solicitacao->status = StatusSolicitacao::REJEITADA;
        } else {
            throw new \InvalidArgumentException('Decisão inválida (APROVAR ou REJEITAR)');
        }

        $solicitacao->decidido_por = $principal->idPessoa;
        $solicitacao->motivo_decisao = $motivo;
        $solicitacao->data_decisao = now()->toDateTimeString();
        $solicitacao->save();

        return $solicitacao->refresh();
    }
}
