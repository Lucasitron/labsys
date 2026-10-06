<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V4__create_access_log_table.sql — acesso físico (auditoria). id_user NULL
// quando o cartão é desconhecido (ACESSO_NEGADO).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('auth.access_log', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_user')->nullable();
            $table->string('uuid_rfid');
            $table->timestampTz('timestamp');
            $table->string('type', 32);
            $table->index(['uuid_rfid', 'timestamp'], 'ix_access_log_uuid');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('auth.access_log');
    }
};
