<?php

namespace Tests\Unit\Auth;

use App\Modules\Auth\Enums\AccessLogType;
use App\Modules\Auth\Services\AccessLogService;
use Tests\TestCase;

class AccessLogServiceTest extends TestCase
{
    public function test_sem_anterior_considera_entrada(): void
    {
        $this->assertSame(AccessLogType::ENTRADA, app(AccessLogService::class)->resolveType('cartao-novo'));
    }

    public function test_alterna_entrada_saida_por_cartao(): void
    {
        $svc = app(AccessLogService::class);

        $svc->record(1, 'cartao-a', AccessLogType::ENTRADA);
        $this->assertSame(AccessLogType::SAIDA, $svc->resolveType('cartao-a'));

        $svc->record(1, 'cartao-a', AccessLogType::SAIDA);
        $this->assertSame(AccessLogType::ENTRADA, $svc->resolveType('cartao-a'));

        // cartão desconhecido não interfere
        $this->assertSame(AccessLogType::ENTRADA, $svc->resolveType('cartao-b'));
    }

    public function test_record_persiste_tipo_negado_com_id_user_null(): void
    {
        $log = app(AccessLogService::class)->record(null, 'cartao-x', AccessLogType::ACESSO_NEGADO);

        $this->assertNull($log->id_user);
        $this->assertSame(AccessLogType::ACESSO_NEGADO, $log->type);
        $this->assertNotNull($log->timestamp);
    }
}
