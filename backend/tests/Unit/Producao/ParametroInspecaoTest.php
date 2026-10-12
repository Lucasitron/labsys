<?php

namespace Tests\Unit\Producao;

use App\Modules\Producao\Enums\StatusInspecao;
use App\Modules\Producao\Models\Inspecao5S;
use App\Modules\Producao\Models\ItemInspecao5S;
use App\Modules\Producao\Models\Parametro5S;
use Tests\TestCase;

/** Parâmetro (flag experimental/inteiro) + `recalcularStatus` (com banco limpo). */
class ParametroInspecaoTest extends TestCase
{
    public function test_seed_v17_traz_parametros_e_experimental_ativo(): void
    {
        $this->assertSame('15', Parametro5S::valorDe('diasParaAuditoriaProjeto'));
        $this->assertSame(15, Parametro5S::inteiroDe('diasParaAuditoriaProjeto', 99));
        $this->assertSame(99, Parametro5S::inteiroDe('chaveInexistente', 99));
        $this->assertTrue(Parametro5S::experimentalAtivo());
    }

    public function test_recalculado_ok_so_quando_tudo_conforme(): void
    {
        $inspecao = new Inspecao5S;
        $inspecao->setRelation('itens', collect([
            new ItemInspecao5S(['conforme' => true]),
            new ItemInspecao5S(['conforme' => true]),
        ]));

        $this->assertSame(StatusInspecao::OK, $inspecao->recalculado());

        $inspecao->setRelation('itens', collect([
            new ItemInspecao5S(['conforme' => true]),
            new ItemInspecao5S(['conforme' => false]),
        ]));

        $this->assertSame(StatusInspecao::NAO_CONFORME, $inspecao->recalculado());
    }
}
