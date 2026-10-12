<?php

namespace Tests\Unit\Rh;

use App\Modules\Rh\Rules\CpfRule;
use PHPUnit\Framework\TestCase;

class CpfRuleTest extends TestCase
{
    public function test_valido_aceita_formatado_e_puro(): void
    {
        $this->assertTrue(CpfRule::valido('529.982.247-25'));
        $this->assertTrue(CpfRule::valido('52998224725'));
    }

    public function test_valido_rejeita_digito_errado_e_triviais(): void
    {
        $this->assertFalse(CpfRule::valido('529.982.247-26'));
        $this->assertFalse(CpfRule::valido('111.111.111-11'));
        $this->assertFalse(CpfRule::valido('123'));
        $this->assertFalse(CpfRule::valido(''));
    }

    public function test_normalizar_e_mascarar_lgpd(): void
    {
        $this->assertSame('52998224725', CpfRule::normalizar('529.982.247-25'));
        $this->assertNull(CpfRule::normalizar(null));
        $this->assertNull(CpfRule::normalizar('   '));

        $this->assertSame('***.***.***-25', CpfRule::mascarar('52998224725'));
        $this->assertNull(CpfRule::mascarar(null));
        $this->assertNull(CpfRule::mascarar('curto'));
    }
}
