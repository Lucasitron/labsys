<?php

namespace Tests\Unit\Vendas;

use App\Modules\Vendas\Services\MarketplaceService;
use PHPUnit\Framework\TestCase;

class MarketplaceLiquidoTest extends TestCase
{
    public function test_liquido_bruto_menos_taxa(): void
    {
        $service = new MarketplaceService;

        $this->assertSame('90.00', $service->liquido('100.00', '10.00'));
        $this->assertSame('0.00', $service->liquido('50.00', '50.00'));
    }
}
