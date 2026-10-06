<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V14__apontamento_horario_motivo.sql — horário (recálculo) + motivo de rejeição.
// (Gap V13 não existe upstream — nenhuma migration inventada.)
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('rh.apontamento_horas', function (Blueprint $table): void {
            $table->time('hora_inicio')->nullable();
            $table->time('hora_fim')->nullable();
            $table->text('motivo_rejeicao')->nullable();
        });
    }

    public function down(): void
    {
        Schema::table('rh.apontamento_horas', function (Blueprint $table): void {
            $table->dropColumn(['hora_inicio', 'hora_fim', 'motivo_rejeicao']);
        });
    }
};
