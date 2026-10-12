<?php

namespace Tests\Unit\Auth;

use App\Modules\Auth\Models\ParametroSistema;
use App\Modules\Auth\Services\SistemaService;
use App\Shared\Exceptions\ConfiguracaoInvalidaException;
use Tests\TestCase;

class SistemaServiceTest extends TestCase
{
    public function test_obter_traz_defaults_e_tokens(): void
    {
        $config = app(SistemaService::class)->obter();

        $this->assertSame('FabLab IFPR — Curitiba', $config['identidade']['nomeFablab']);
        $this->assertSame('Semanal', $config['cadenciaChecklist5S']);
        $this->assertSame('Mensal', $config['cadenciaAuditoria5S']);
        $this->assertSame([], $config['tokens']);
        $this->assertSame(4, ParametroSistema::count());
    }

    public function test_atualizar_persiste_e_normaliza_cadencia(): void
    {
        $config = app(SistemaService::class)->atualizar([
            'identidade' => ['nomeFablab' => 'Lab X', 'logo' => 'logo.png'],
            'cadenciaChecklist5S' => 'quinzenal',
            'cadenciaAuditoria5S' => null,
        ]);

        $this->assertSame('Lab X', $config['identidade']['nomeFablab']);
        $this->assertSame('Quinzenal', $config['cadenciaChecklist5S']);
        $this->assertNull($config['cadenciaAuditoria5S']);
    }

    /** @dataProvider casosInvalidos */
    public function test_atualizar_invalido_lanca_422(array $data): void
    {
        $this->expectException(ConfiguracaoInvalidaException::class);
        app(SistemaService::class)->atualizar($data);
    }

    /** @return array<string, array{0:array}> */
    public static function casosInvalidos(): array
    {
        return [
            'sem identidade' => [[]],
            'nome vazio' => [['identidade' => ['nomeFablab' => '  '], 'cadenciaChecklist5S' => 'Semanal']],
            'cadencia obrigatoria ausente' => [['identidade' => ['nomeFablab' => 'Lab'], 'cadenciaChecklist5S' => null]],
            'cadencia invalida' => [['identidade' => ['nomeFablab' => 'Lab'], 'cadenciaChecklist5S' => 'Diaria']],
            'auditoria invalida' => [['identidade' => ['nomeFablab' => 'Lab'], 'cadenciaChecklist5S' => 'Semanal', 'cadenciaAuditoria5S' => 'Anual']],
        ];
    }
}
