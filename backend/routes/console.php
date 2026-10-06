<?php

use App\Modules\Auth\Services\TokenBlacklistService;
use Illuminate\Support\Facades\Schedule;

// Auth: purge diário 03:00 — remove só tokens expirados da blacklist
// (equivale ao @Scheduled "0 0 3 * * *" do TokenBlacklistCleanupTask).
Schedule::call(
    fn () => app(TokenBlacklistService::class)->cleanupExpired()
)->dailyAt('03:00')->name('auth:purge-blacklist');
