<?php

namespace App\Modules\Dashboard\Services;

use App\Modules\Auth\Enums\Role;
use App\Modules\Dashboard\Policies\DashboardPolicy;
use App\Modules\Estoque\Contracts\EstoqueContract;
use App\Modules\Notification\Contracts\NotificationContract;
use App\Modules\Producao\Contracts\ProducaoContract;
use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Enums\MaquinaStatus;
use App\Modules\Rh\Contracts\RhContract;
use Illuminate\Support\Carbon;

/**
 * Agregador do dashboard (sem persistência): compõe o `DashboardSummary` via
 * Contracts in-process. O filtro `restricted` é aplicado aqui no backend —
 * chaves omitidas p/ não-Admin (o front apenas oculta).
 */
class DashboardService
{
    public function __construct(
        private ProducaoContract $producao,
        private RhContract $rh,
        private EstoqueContract $estoque,
        private NotificationContract $notificacoes,
    ) {}

    /**
     * @return array{tasks:list<array{id:string,title:string,module:string,due:?string,dueLabel:string,urgent:bool}>,kpis:array<string,array{value:int,total:?int,delta:?string,deltaTone:string,restricted:bool}>,ordersByStatus:list<array{status:string,label:string,count:int,color:string}>,machinesByStatus:list<array{status:string,label:string,count:int,color:string}>,activity:list<array{id:string,actor:string,verb:string,target:string,module:string,at:string}>}
     */
    public function resumo(int $idUsuario, ?Role $role): array
    {
        DashboardPolicy::exigeResumo($role);
        $admin = $role === Role::ADMIN;

        $kanban = $this->producao->resumoKanban();

        $kpis = [
            'ordersActive' => $this->kpi($this->ordensAtivas($kanban), null, false),
            'notificationsUnread' => $this->kpi($this->notificacoes->naoLidas($idUsuario), null, false),
        ];

        $maquinas = $admin ? $this->producao->resumoMaquinas() : [];
        if ($admin) {
            $kpis['loansOpen'] = $this->kpi($this->estoque->emprestimosAbertos(), null, true);
            $kpis['machinesActive'] = $this->kpi(
                $this->maquinasAtivas($maquinas), array_sum($maquinas), true,
            );
        }

        return [
            'tasks' => $this->tarefas($idUsuario),
            'kpis' => $kpis,
            'ordersByStatus' => $this->fatiasKanban($kanban),
            'machinesByStatus' => $admin ? $this->fatiasMaquinas($maquinas) : [],
            'activity' => $this->atividade($idUsuario, $admin),
        ];
    }

    /** @return list<array{id:string,title:string,module:string,due:?string,dueLabel:string,urgent:bool}> */
    private function tarefas(int $idUsuario): array
    {
        return array_map(fn (array $tarefa) => array_merge([
            'id' => (string) $tarefa['id'],
            'title' => $tarefa['titulo'],
            'module' => 'producao',
            'due' => $tarefa['due'] ?? null,
        ], $this->prazo($tarefa['due'] ?? null)),
            $this->producao->tarefasAtivasPorResponsavel($idUsuario));
    }

    /** `dueLabel` (`Hoje`/`dd/MM`/`Sem prazo`) + `urgent` (vence hoje) servidos prontos. */
    private function prazo(?string $due): array
    {
        if ($due === null) {
            return ['dueLabel' => 'Sem prazo', 'urgent' => false];
        }

        if ($due === today()->toDateString()) {
            return ['dueLabel' => 'Hoje', 'urgent' => true];
        }

        return ['dueLabel' => Carbon::parse($due)->format('d/m'), 'urgent' => false];
    }

    /** @return array{value:int,total:?int,delta:?string,deltaTone:string,restricted:bool} */
    private function kpi(int $value, ?int $total, bool $restricted): array
    {
        return ['value' => $value, 'total' => $total, 'delta' => null, 'deltaTone' => 'muted', 'restricted' => $restricted];
    }

    private function ordensAtivas(array $kanban): int
    {
        $ativas = 0;
        foreach ($kanban as $status => $total) {
            if ($status !== KanbanStatus::ENTREGUE->value) {
                $ativas += (int) $total;
            }
        }

        return $ativas;
    }

    private function maquinasAtivas(array $resumo): int
    {
        return (int) ($resumo[MaquinaStatus::DISPONIVEL->value] ?? 0)
            + (int) ($resumo[MaquinaStatus::EM_USO->value] ?? 0);
    }

    /** Pipeline fixo de 5 colunas (Kanban LINKA o Vendas — sem agregação duplicada). */
    private function fatiasKanban(array $kanban): array
    {
        return array_map(fn (KanbanStatus $status) => [
            'status' => strtolower($status->value),
            'label' => $status->rotuloVendas(),
            'count' => (int) ($kanban[$status->value] ?? 0),
            'color' => match ($status) {
                KanbanStatus::FILA => 'muted',
                KanbanStatus::PRODUCAO => 'brand',
                KanbanStatus::ACABAMENTO => 'warn',
                KanbanStatus::PRONTO => 'success',
                KanbanStatus::ENTREGUE => 'muted',
            },
        ], KanbanStatus::cases());
    }

    /** Agrupa as máquinas por status operacional (só observados, como no Java). */
    private function fatiasMaquinas(array $resumo): array
    {
        $fatias = [];
        foreach ($resumo as $status => $total) {
            $total = (int) $total;
            if ($total <= 0) {
                continue;
            }

            $fatias[] = match ($status) {
                MaquinaStatus::DISPONIVEL->value => ['status' => 'disponivel', 'label' => 'Disponíveis', 'count' => $total, 'color' => 'success'],
                MaquinaStatus::EM_USO->value => ['status' => 'em_uso', 'label' => 'Em uso', 'count' => $total, 'color' => 'brand'],
                MaquinaStatus::MANUTENCAO->value => ['status' => 'manutencao', 'label' => 'Em manutenção', 'count' => $total, 'color' => 'warn'],
                default => ['status' => strtolower((string) $status), 'label' => (string) $status, 'count' => $total, 'color' => 'muted'],
            };
        }

        return $fatias;
    }

    /** Solicitações (+emitidos se Admin) → timeline top 8 desc. */
    private function atividade(int $idUsuario, bool $admin): array
    {
        $solicitacoes = $this->rh->solicitacoesCertificado('PENDENTE', $admin ? null : $idUsuario);
        $emitidos = $admin ? $this->rh->certificadosEmitidos() : [];

        $itens = array_merge(
            array_map(fn (array $s) => [
                'id' => 'act-sol-'.$s['idSolicitacao'],
                'actor' => $s['nomeFuncionario'],
                'verb' => 'solicitou certificado de horas por',
                'target' => $s['horasSolicitadas'].' h ('.$s['tipoCertificado'].')',
                'module' => 'rh',
                'at' => $s['dataSolicitacao'],
            ], $solicitacoes),
            array_map(fn (array $e) => [
                'id' => 'act-emit-'.$e['idCertificado'],
                'actor' => 'Sistema RH',
                'verb' => 'emitiu certificado para',
                'target' => $e['nomeFuncionario'],
                'module' => 'rh',
                'at' => $e['dataEmissao'],
            ], $emitidos),
        );

        usort($itens, fn (array $a, array $b) => strcmp($b['at'], $a['at']));

        return array_slice(array_values($itens), 0, 8);
    }
}
