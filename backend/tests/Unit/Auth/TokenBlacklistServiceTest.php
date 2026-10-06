<?php

namespace Tests\Unit\Auth;

use App\Modules\Auth\Models\TokenBlacklist;
use App\Modules\Auth\Services\TokenBlacklistService;
use Tests\TestCase;

class TokenBlacklistServiceTest extends TestCase
{
    public function test_blacklist_e_idempotente(): void
    {
        $svc = app(TokenBlacklistService::class);

        $this->assertFalse($svc->isBlacklisted('tok-1'));
        $svc->blacklist('tok-1', now()->addMinutes(15));
        $svc->blacklist('tok-1', now()->addMinutes(15));

        $this->assertTrue($svc->isBlacklisted('tok-1'));
        $this->assertSame(1, TokenBlacklist::where('token', 'tok-1')->count());
    }

    public function test_cleanup_remove_so_expirados(): void
    {
        $svc = app(TokenBlacklistService::class);
        $svc->blacklist('expirado', now()->subMinute());
        $svc->blacklist('valido', now()->addHour());

        $this->assertSame(1, $svc->cleanupExpired());
        $this->assertFalse($svc->isBlacklisted('expirado'));
        $this->assertTrue($svc->isBlacklisted('valido'));
    }
}
