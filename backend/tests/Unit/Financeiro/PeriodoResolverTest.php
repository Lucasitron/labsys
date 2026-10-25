<?php

namespace Tests\Unit\Financeiro;

use App\Modules\Financeiro\Services\RelatorioService;
use PHPUnit\Framework\TestCase;

/** resolverPeriodo: dataInicio/dataFim vencem periodo; yyyy-MM vira o mês. */
class PeriodoResolverTest extends TestCase
{
    private RelatorioService $relatorios;

    protected function setUp(): void
    {
        parent::setUp();
        $this->relatorios = new RelatorioService();
    }

    public function test_datas_tem_precedencia_sobre_periodo(): void
    {
        $per = $this->relatorios->resolverPeriodo('2026-04-01', '2026-04-30', '2026-03');

        $this->assertSame(['inicio' => '2026-04-01', 'fim' => '2026-04-30'], $per);
    }

    public function test_periodo_vira_primeiro_e_ultimo_dia_do_mes(): void
    {
        $per = $this->relatorios->resolverPeriodo(null, null, '2026-02');

        $this->assertSame(['inicio' => '2026-02-01', 'fim' => '2026-02-28'], $per);
    }

    public function test_sem_nada_retorna_nulos(): void
    {
        $per = $this->relatorios->resolverPeriodo(null, null, null);

        $this->assertSame(['inicio' => null, 'fim' => null], $per);
    }

    public function test_data_parcial_preserva_nulo(): void
    {
        $per = $this->relatorios->resolverPeriodo('2026-01-01', null, null);

        $this->assertSame(['inicio' => '2026-01-01', 'fim' => null], $per);
    }
}
