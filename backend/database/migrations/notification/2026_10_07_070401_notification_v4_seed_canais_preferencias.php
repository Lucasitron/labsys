<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

// V4 = seeds idempotentes por upsert, DENTRO da migration (padrão Auth V6/V7):
// canais (EMAIL on / WHATSAPP off, `parametros` = {} — SMTP real vem do `.env`,
// nunca do seed) + matriz default do Java (tudo true exceto `sistema/push`).
return new class extends Migration
{
    public function up(): void
    {
        DB::table('notification.configuracao_canal')->upsert(
            [
                ['canal' => 'EMAIL', 'habilitado' => true, 'parametros' => '{}'],
                ['canal' => 'WHATSAPP', 'habilitado' => false, 'parametros' => '{}'],
            ],
            ['canal'],
            ['habilitado', 'parametros'],
        );

        $tipos = ['encomenda', 'estoque', 'financeiro', 'producao', 'pessoas', 'sistema'];
        $canais = ['inapp', 'email', 'push'];
        $linhas = [];
        foreach ($tipos as $tipo) {
            foreach ($canais as $canal) {
                $linhas[] = [
                    'tipo' => $tipo,
                    'canal' => $canal,
                    'habilitado' => ! ($tipo === 'sistema' && $canal === 'push'),
                ];
            }
        }

        DB::table('notification.preferencia_notificacao')->upsert(
            $linhas,
            ['tipo', 'canal'],
            ['habilitado'],
        );
    }

    public function down(): void
    {
        // Seed idempotente: down não remove (padrão Auth V6/V7).
    }
};
