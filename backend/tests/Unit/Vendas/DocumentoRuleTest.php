<?php

namespace Tests\Unit\Vendas;

use App\Modules\Vendas\Rules\DocumentoRule;
use PHPUnit\Framework\TestCase;

class DocumentoRuleTest extends TestCase
{
    public function test_valido_aceita_cpf_e_cnpj_formatados_e_puros(): void
    {
        $this->assertTrue(DocumentoRule::valido('529.982.247-25'));
        $this->assertTrue(DocumentoRule::valido('52998224725'));
        $this->assertTrue(DocumentoRule::valido('11.444.777/0001-61'));
        $this->assertTrue(DocumentoRule::valido('11444777000161'));
    }

    public function test_valido_rejeita_digito_errado_triviais_e_tamanho(): void
    {
        $this->assertFalse(DocumentoRule::valido('529.982.247-26'));
        $this->assertFalse(DocumentoRule::valido('111.111.111-11'));
        $this->assertFalse(DocumentoRule::valido('11.444.777/0001-62'));
        $this->assertFalse(DocumentoRule::valido('00000000000000'));
        $this->assertFalse(DocumentoRule::valido('123'));
        $this->assertFalse(DocumentoRule::valido(''));
    }

    public function test_normalizar_e_mascarar_lgpd(): void
    {
        $this->assertSame('52998224725', DocumentoRule::normalizar('529.982.247-25'));
        $this->assertSame('11444777000161', DocumentoRule::normalizar('11.444.777/0001-61'));
        $this->assertNull(DocumentoRule::normalizar(null));
        $this->assertNull(DocumentoRule::normalizar('   '));

        $this->assertSame('***.982.247-**', DocumentoRule::mascarar('52998224725'));
        $this->assertSame('**. 444.777/0001-**', DocumentoRule::mascarar('11.444.777/0001-61'));
        $this->assertNull(DocumentoRule::mascarar(null));
        $this->assertSame('***', DocumentoRule::mascarar('12345'));
    }
}
