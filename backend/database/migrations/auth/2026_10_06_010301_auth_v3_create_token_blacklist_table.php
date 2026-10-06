<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V3__create_token_blacklist_table.sql — tokens JWT revogados (logout/rotação).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('auth.token_blacklist', function (Blueprint $table): void {
            $table->id();
            $table->text('token')->unique('uk_token_blacklist_token');
            $table->timestampTz('expiry_date');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('auth.token_blacklist');
    }
};
