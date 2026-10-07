<?php

namespace Tests\Unit\Dashboard;

use App\Modules\Auth\Enums\Role;
use App\Modules\Dashboard\Services\DashboardService;
use App\Modules\Estoque\Contracts\EstoqueContract;
use App\Modules\Notification\Contracts\NotificationContract;
use App\Modules\Producao\Contracts\ProducaoContract;
use App\Modules\Rh\Contracts\RhContract;
use App\Shared\Exceptions\ForbiddenException;
use Tests\TestCase;

class FakeRhContract implements RhContract
{
    /** @var list<array{idSolicitacao:int,nomeFuncionario:string,tipoCertificado:string,horasSolicitadas:string,dataSolicitacao:string,status:string}> */
    public array $solicitacoes = [];

    /** @var list<array{idCertificado:int,nomeFuncionario:string,dataEmissao:string}> */
    public array $emitidos = [];

    public function funcionarioExiste(int $idFuncionario): bool
    {
        return true;
    }

    public function horasValidadasNoPeriodo(int $idFuncionario, string $tipo, string $inicio, string $fim): string
    {
        return '0.00';
    }

    public function incoerencias(int $idFuncionario): array
    {
        return [];
    }

    public function nomePessoa(int $idPessoa): ?string
    {
        return null;
    }

    public function dadosDestinatario(int $idFuncionario): ?array
    {
        return null;
    }

    public function solicitacoesCertificado(?string $status = null, ?int $idUsuario = null): array
    {
        $linhas = array_values(array_filter(
            $this->solicitacoes,
            fn (array $s) => ($status === null || $s['status'] === $status)
                && ($idUsuario === null || ($s['idUsuario'] ?? null) === $idUsuario),
        ));

        return array_map(function (array $s): array {
            unset($s['idUsuario']);

            return $s;
        }, $linhas);
    }

    public function certificadosEmitidos(): array
    {
        return $this->emitidos;
    }
}

class FakeEstoqueContract implements EstoqueContract
{
    public int $abertos = 0;

    public function itemExiste(int $idItem): bool
    {
        return true;
    }

    public function saldoDe(int $idItem): ?string
    {
        return '0.00';
    }

    public function baixarConsumo(array $itens, int $idReferencia): array
    {
        return [];
    }

    public function emprestimosAbertos(): int
    {
        return $this->abertos;
    }
}

class FakeProducaoContract implements ProducaoContract
{
    /** @var array<string,int> */
    public array $kanban = [];

    /** @var array<string,int> */
    public array $maquinas = [];

    /** @var list<array{id:int,titulo:string,due:?string}> */
    public array $tarefas = [];

    public function resumoKanban(): array
    {
        return $this->kanban;
    }

    public function tarefasAtivasPorResponsavel(int $idResponsavel): array
    {
        return array_values(array_filter(
            $this->tarefas,
            fn (array $t) => ($t['idResponsavel'] ?? null) === $idResponsavel,
        ));
    }

    public function resumoMaquinas(): array
    {
        return $this->maquinas;
    }
}

class FakeNotificationContract implements NotificationContract
{
    public int $naoLidas = 0;

    public function naoLidas(int $idUsuario): int
    {
        return $this->naoLidas;
    }

    public function inbox(int $idUsuario, int $page, int $size, ?string $tipo = null, ?bool $lida = null): array
    {
        return ['items' => collect(), 'total' => 0, 'page' => $page, 'size' => $size, 'pageSize' => $size, 'totalPages' => 0];
    }
}

/** Composição do resumo (restricted por papel, derivações servidas prontas). */
class DashboardServiceTest extends TestCase
{
    private FakeRhContract $rh;

    private FakeEstoqueContract $estoque;

    private FakeProducaoContract $producao;

    private FakeNotificationContract $notificacoes;

    private DashboardService $resumo;

    protected function setUp(): void
    {
        parent::setUp();
        $this->rh = new FakeRhContract;
        $this->estoque = new FakeEstoqueContract;
        $this->producao = new FakeProducaoContract;
        $this->notificacoes = new FakeNotificationContract;
        $this->resumo = new DashboardService($this->producao, $this->rh, $this->estoque, $this->notificacoes);
    }

    public function test_recrutando_e_sem_papel_403(): void
    {
        foreach ([Role::RECRUTANDO, null] as $role) {
            try {
                $this->resumo->resumo(7, $role);
                $this->fail('deveria lançar 403');
            } catch (ForbiddenException) {
                $this->assertTrue(true);
            }
        }
    }

    public function test_admin_completo_com_chaves_restricted(): void
    {
        $hoje = today()->toDateString();
        $amanha = today()->addDay()->toDateString();
        $this->producao->tarefas = [
            ['id' => 11, 'idResponsavel' => 7, 'titulo' => 'Cortar MDF', 'due' => $hoje],
            ['id' => 12, 'idResponsavel' => 7, 'titulo' => 'Lixar', 'due' => $amanha],
            ['id' => 13, 'idResponsavel' => 7, 'titulo' => 'Sem prazo', 'due' => null],
            ['id' => 14, 'idResponsavel' => 99, 'titulo' => 'De outro', 'due' => null],
        ];
        $this->producao->kanban = ['FILA' => 2, 'PRODUCAO' => 1, 'ACABAMENTO' => 0, 'PRONTO' => 0, 'ENTREGUE' => 3];
        $this->producao->maquinas = ['DISPONIVEL' => 2, 'EM_USO' => 1, 'MANUTENCAO' => 1, 'QUEBRADA' => 1];
        $this->estoque->abertos = 4;
        $this->notificacoes->naoLidas = 5;
        $this->rh->solicitacoes = [
            $this->solicitacao(1, '2026-10-01T10:00:00+00:00'),
            $this->solicitacao(2, '2026-10-02T10:00:00+00:00'),
        ];
        $this->rh->emitidos = [['idCertificado' => 9, 'nomeFuncionario' => 'Ana', 'dataEmissao' => '2026-10-03T10:00:00+00:00']];

        $resumo = $this->resumo->resumo(7, Role::ADMIN);

        $this->assertCount(3, $resumo['tasks']);
        $this->assertSame('11', $resumo['tasks'][0]['id']);
        $this->assertSame('producao', $resumo['tasks'][0]['module']);
        $this->assertSame(['dueLabel' => 'Hoje', 'urgent' => true],
            ['dueLabel' => $resumo['tasks'][0]['dueLabel'], 'urgent' => $resumo['tasks'][0]['urgent']]);
        $this->assertSame($amanha, $resumo['tasks'][1]['due']);
        $this->assertSame(today()->addDay()->format('d/m'), $resumo['tasks'][1]['dueLabel']);
        $this->assertFalse($resumo['tasks'][1]['urgent']);
        $this->assertSame('Sem prazo', $resumo['tasks'][2]['dueLabel']);

        $this->assertSame(3, $resumo['kpis']['ordersActive']['value']);
        $this->assertFalse($resumo['kpis']['ordersActive']['restricted']);
        $this->assertSame(5, $resumo['kpis']['notificationsUnread']['value']);
        $this->assertSame(4, $resumo['kpis']['loansOpen']['value']);
        $this->assertTrue($resumo['kpis']['loansOpen']['restricted']);
        $this->assertSame(['value' => 3, 'total' => 5], [
            'value' => $resumo['kpis']['machinesActive']['value'],
            'total' => $resumo['kpis']['machinesActive']['total'],
        ]);
        $this->assertSame('muted', $resumo['kpis']['machinesActive']['deltaTone']);
        $this->assertNull($resumo['kpis']['machinesActive']['delta']);

        $this->assertCount(5, $resumo['ordersByStatus']);
        $this->assertSame(['status' => 'fila', 'label' => 'Fila', 'count' => 2, 'color' => 'muted'], $resumo['ordersByStatus'][0]);
        $this->assertSame('brand', $resumo['ordersByStatus'][1]['color']);

        $this->assertSame([
            ['status' => 'disponivel', 'label' => 'Disponíveis', 'count' => 2, 'color' => 'success'],
            ['status' => 'em_uso', 'label' => 'Em uso', 'count' => 1, 'color' => 'brand'],
            ['status' => 'manutencao', 'label' => 'Em manutenção', 'count' => 1, 'color' => 'warn'],
            ['status' => 'quebrada', 'label' => 'QUEBRADA', 'count' => 1, 'color' => 'muted'],
        ], $resumo['machinesByStatus']);

        $this->assertSame(['act-emit-9', 'act-sol-2', 'act-sol-1'],
            array_column($resumo['activity'], 'id'));
        $this->assertSame('solicitou certificado de horas por', $resumo['activity'][1]['verb']);
        $this->assertSame('5.00 h (EXTENSAO)', $resumo['activity'][1]['target']);
        $this->assertSame('Sistema RH', $resumo['activity'][0]['actor']);
    }

    public function test_nao_admin_sem_chaves_restricted_e_so_proprias_solicitacoes(): void
    {
        $this->producao->tarefas = [
            ['id' => 21, 'idResponsavel' => 8, 'titulo' => 'Minha', 'due' => null],
        ];
        $this->producao->kanban = ['FILA' => 1, 'ENTREGUE' => 1];
        $this->rh->solicitacoes = [
            array_merge($this->solicitacao(3, '2026-10-04T10:00:00+00:00'), ['idUsuario' => 8]),
            array_merge($this->solicitacao(4, '2026-10-05T10:00:00+00:00'), ['idUsuario' => 77]),
        ];

        foreach ([Role::BOLSISTA, Role::VOLUNTARIO, Role::ESTAGIARIO] as $role) {
            $resumo = $this->resumo->resumo(8, $role);

            $this->assertArrayNotHasKey('loansOpen', $resumo['kpis']);
            $this->assertArrayNotHasKey('machinesActive', $resumo['kpis']);
            $this->assertSame([], $resumo['machinesByStatus']);
            $this->assertSame(1, $resumo['kpis']['ordersActive']['value']);
            $this->assertCount(1, $resumo['tasks']);
            $this->assertSame(['act-sol-3'], array_column($resumo['activity'], 'id'));
        }
    }

    public function test_admin_vazio_com_chaves_presentes_e_zeros(): void
    {
        $resumo = $this->resumo->resumo(7, Role::ADMIN);

        $this->assertSame([], $resumo['tasks']);
        $this->assertSame(0, $resumo['kpis']['ordersActive']['value']);
        $this->assertSame(0, $resumo['kpis']['loansOpen']['value']);
        $this->assertSame(0, $resumo['kpis']['machinesActive']['value']);
        $this->assertSame(0, $resumo['kpis']['notificationsUnread']['value']);
        $this->assertSame([], $resumo['machinesByStatus']);
        $this->assertSame([], $resumo['activity']);
        $this->assertCount(5, $resumo['ordersByStatus']);
    }

    public function test_activity_limita_top_8_desc(): void
    {
        for ($i = 1; $i <= 10; $i++) {
            $this->rh->solicitacoes[] = $this->solicitacao($i, sprintf('2026-09-%02dT10:00:00+00:00', $i));
        }

        $resumo = $this->resumo->resumo(7, Role::ADMIN);

        $this->assertCount(8, $resumo['activity']);
        $this->assertSame('act-sol-10', $resumo['activity'][0]['id']);
        $this->assertSame('act-sol-3', $resumo['activity'][7]['id']);
    }

    /** @return array{idSolicitacao:int,nomeFuncionario:string,tipoCertificado:string,horasSolicitadas:string,dataSolicitacao:string,status:string} */
    private function solicitacao(int $id, string $at): array
    {
        return [
            'idSolicitacao' => $id,
            'nomeFuncionario' => 'Beto',
            'tipoCertificado' => 'EXTENSAO',
            'horasSolicitadas' => '5.00',
            'dataSolicitacao' => $at,
            'status' => 'PENDENTE',
        ];
    }
}
