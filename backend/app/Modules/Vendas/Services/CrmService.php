<?php

namespace App\Modules\Vendas\Services;

use App\Modules\Vendas\Models\Cliente;
use App\Modules\Vendas\Models\InteracaoCliente;
use App\Modules\Vendas\Models\TarefaMarketing;
use App\Modules\Vendas\Policies\VendasPolicy;
use App\Modules\Vendas\VendasPrincipal;
use App\Shared\Exceptions\ResourceNotFoundException;

/** CRM interno: interações (timeline) e tarefas de marketing (regras só aqui). */
class CrmService
{
    public const TIPOS = ['E-mail', 'Telefone', 'Reunião', 'WhatsApp'];

    public const STATUS = ['Pendente', 'Em Andamento', 'Concluída'];

    public const PRIORIDADES = ['Baixa', 'Média', 'Alta'];

    public function registrarInteracao(array $dados, VendasPrincipal $principal): InteracaoCliente
    {
        VendasPolicy::exigeEscrita($principal);

        Cliente::find($dados['clienteId'])
            ?? throw new ResourceNotFoundException("Cliente não encontrado: {$dados['clienteId']}");

        if (! in_array($dados['tipo'], self::TIPOS, true)) {
            throw new \InvalidArgumentException('Tipo de interação inválido (E-mail, Telefone, Reunião ou WhatsApp)');
        }

        return InteracaoCliente::create([
            'id_cliente' => $dados['clienteId'],
            'data_interacao' => $dados['dataInteracao'] ?? now()->toDateTimeString(),
            'tipo' => $dados['tipo'],
            'descricao' => $dados['descricao'],
            'id_usuario' => $principal->idPessoa,
        ]);
    }

    /** @return list<InteracaoCliente> */
    public function listarInteracoes(int $idCliente, VendasPrincipal $principal): array
    {
        VendasPolicy::exigeLeitura($principal);

        Cliente::find($idCliente)
            ?? throw new ResourceNotFoundException("Cliente não encontrado: {$idCliente}");

        return InteracaoCliente::where('id_cliente', $idCliente)
            ->orderByDesc('data_interacao')
            ->get()
            ->all();
    }

    public function criarTarefa(array $dados, VendasPrincipal $principal): TarefaMarketing
    {
        VendasPolicy::exigeEscrita($principal);

        $status = $dados['status'] ?? 'Pendente';
        $prioridade = $dados['prioridade'] ?? 'Média';
        $this->validarTarefa($status, $prioridade);

        if (($dados['dataInicio'] ?? null) !== null && ($dados['dataFim'] ?? null) !== null
            && $dados['dataFim'] < $dados['dataInicio']) {
            throw new \InvalidArgumentException('O prazo não pode ser anterior ao início');
        }

        return TarefaMarketing::create([
            'titulo' => $dados['titulo'],
            'descricao' => $dados['descricao'] ?? null,
            'id_responsavel' => $dados['responsavelId'],
            'data_inicio' => $dados['dataInicio'] ?? null,
            'data_fim' => $dados['dataFim'] ?? null,
            'status' => $status,
            'prioridade' => $prioridade,
            'criado_por' => $principal->idPessoa,
        ]);
    }

    /** @return array{tarefas:list<TarefaMarketing>,counts:array<string,int>} */
    public function listarTarefas(?int $responsavelId, ?string $status, VendasPrincipal $principal): array
    {
        VendasPolicy::exigeLeitura($principal);

        if ($status !== null && trim($status) !== '' && ! in_array($status, self::STATUS, true)) {
            throw new \InvalidArgumentException('Status inválido (Pendente, Em Andamento ou Concluída)');
        }

        $tarefas = TarefaMarketing::query()
            ->when($responsavelId !== null, fn ($q) => $q->where('id_responsavel', $responsavelId))
            ->when($status !== null && trim($status) !== '', fn ($q) => $q->where('status', $status))
            ->orderByDesc('id_tarefa')
            ->get();

        $counts = [];
        foreach (self::STATUS as $caso) {
            $counts[$caso] = TarefaMarketing::where('status', $caso)->count();
        }

        return ['tarefas' => $tarefas->all(), 'counts' => $counts];
    }

    public function atualizarTarefa(int $id, array $dados, VendasPrincipal $principal): TarefaMarketing
    {
        VendasPolicy::exigeEscrita($principal);

        $tarefa = TarefaMarketing::find($id)
            ?? throw new ResourceNotFoundException("Tarefa não encontrada: {$id}");

        if (($dados['status'] ?? null) !== null) {
            $this->validarTarefa($dados['status'], $tarefa->prioridade);
            $tarefa->status = $dados['status'];
        }
        if (($dados['prioridade'] ?? null) !== null) {
            $this->validarTarefa($tarefa->status, $dados['prioridade']);
            $tarefa->prioridade = $dados['prioridade'];
        }
        if (($dados['dataFim'] ?? null) !== null) {
            $tarefa->data_fim = $dados['dataFim'];
        }
        $tarefa->save();

        return $tarefa->refresh();
    }

    private function validarTarefa(string $status, string $prioridade): void
    {
        if (! in_array($status, self::STATUS, true)) {
            throw new \InvalidArgumentException('Status inválido (Pendente, Em Andamento ou Concluída)');
        }
        if (! in_array($prioridade, self::PRIORIDADES, true)) {
            throw new \InvalidArgumentException('Prioridade inválida (Baixa, Média ou Alta)');
        }
    }
}
