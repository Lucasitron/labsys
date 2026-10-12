<?php

namespace App\Modules\Notification\Services;

use App\Modules\Notification\Enums\StatusEntrega;
use App\Modules\Notification\Models\ConfiguracaoCanal;
use App\Modules\Notification\Models\Notificacao;
use App\Modules\Notification\Models\NotificacaoHistorico;
use App\Modules\Notification\Models\PreferenciaNotificacao;
use App\Modules\Notification\Policies\NotificationPolicy;
use App\Modules\Notification\Support\LinkValidator;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;

/**
 * Consultas e comandos de notificações (port 1:1 do `NotificacaoService` Java,
 * D-1..D-7, + `revisar` que move p/ o histórico — D9/docs/07 §6).
 *
 * Regra só aqui: controllers validam via `FormRequest` e delegam; o isolamento
 * por dono é sempre pelo `idUsuario` do guard (nunca por parâmetro).
 */
class NotificacaoService
{
    /** @var list<string> */
    public const TIPOS = ['encomenda', 'estoque', 'financeiro', 'producao', 'pessoas', 'sistema'];

    /** @var list<string> */
    public const CANAIS = ['inapp', 'email', 'push'];

    public function contarNaoLidas(int $idUsuario): int
    {
        return Notificacao::where('id_usuario', $idUsuario)->where('lida', false)->count();
    }

    /**
     * Lista paginada canônica do usuário (página 1-based, `size` 1..100).
     *
     * @return array{items:\Illuminate\Database\Eloquent\Collection<int,Notificacao>,total:int,page:int,size:int,pageSize:int,totalPages:int}
     */
    public function listar(int $idUsuario, int $page, int $size, ?string $tipo = null, ?bool $lida = null): array
    {
        if ($page < 1) {
            throw new \InvalidArgumentException('Página inválida: use valores a partir de 1');
        }

        if ($size < 1 || $size > 100) {
            throw new \InvalidArgumentException('Tamanho de página inválido: use valores entre 1 e 100');
        }

        $base = Notificacao::where('id_usuario', $idUsuario)
            ->when($this->normalizar($tipo) !== null, fn ($q) => $q->where('tipo', $this->normalizar($tipo)))
            ->when($lida !== null, fn ($q) => $q->where('lida', $lida))
            ->orderBy('criada_em', 'desc');

        $total = (clone $base)->count();
        $items = $base->forPage($page, $size)->get();

        return [
            'items' => $items,
            'total' => $total,
            'page' => $page,
            'size' => $size,
            'pageSize' => $size,
            'totalPages' => $size > 0 ? (int) ceil($total / $size) : 0,
        ];
    }

    public function marcarComoLida(int $id, int $idUsuario, bool $admin): Notificacao
    {
        $notificacao = Notificacao::find($id);

        if ($notificacao === null) {
            throw new ResourceNotFoundException('Notificação não encontrada');
        }

        NotificationPolicy::exigeDonoOuAdmin($notificacao, $idUsuario, $admin);

        $notificacao->lida = true;
        $notificacao->data_leitura = now();
        $notificacao->save();

        return $notificacao;
    }

    public function marcarTodasComoLidas(int $idUsuario): int
    {
        return Notificacao::where('id_usuario', $idUsuario)->where('lida', false)->update(['lida' => true]);
    }

    /** @return array<string,array<string,bool>> */
    public function obterPreferencias(): array
    {
        $salvas = PreferenciaNotificacao::all()->groupBy('tipo');

        $matriz = [];
        foreach (self::TIPOS as $tipo) {
            $matriz[$tipo] = [];
            foreach (self::CANAIS as $canal) {
                $linha = $salvas[$tipo]?->firstWhere('canal', $canal);
                $matriz[$tipo][$canal] = $linha !== null
                    ? (bool) $linha->habilitado
                    : $this->preferenciaPadrao($tipo, $canal);
            }
        }

        return $matriz;
    }

    /**
     * @param array<string,array<string,bool|null>> $novas
     * @return array<string,array<string,bool>>
     */
    public function salvarPreferencias(array $novas): array
    {
        if ($novas === []) {
            throw new \InvalidArgumentException('Preferências inválidas: informe a matriz tipo × canal');
        }

        foreach ($novas as $tipo => $canais) {
            if (! in_array($tipo, self::TIPOS, true)) {
                throw new \InvalidArgumentException("Tipo de notificação inválido: {$tipo}");
            }

            if (! is_array($canais) || $canais === []) {
                throw new \InvalidArgumentException("Preferências inválidas: informe os canais do tipo {$tipo}");
            }

            foreach ($canais as $canal => $valor) {
                if (! in_array($canal, self::CANAIS, true)) {
                    throw new \InvalidArgumentException("Canal inválido: {$canal}");
                }

                if (! is_bool($valor)) {
                    throw new \InvalidArgumentException("Preferências inválidas: valor ausente em {$tipo}/{$canal}");
                }
            }
        }

        DB::transaction(function () use ($novas): void {
            foreach ($novas as $tipo => $canais) {
                foreach ($canais as $canal => $valor) {
                    PreferenciaNotificacao::updateOrCreate(
                        ['tipo' => $tipo, 'canal' => $canal],
                        ['habilitado' => $valor],
                    );
                }
            }
        });

        return $this->obterPreferencias();
    }

    /**
     * Histórico do Admin: lê `notificacao_historico` (pós-revisão, volume baixo)
     * e retorna a lista integral (front pagina client-side — D-4/Java 1:1).
     *
     * @return array{items:\Illuminate\Database\Eloquent\Collection<int,NotificacaoHistorico>,total:int}
     */
    public function historico(?string $busca, ?string $tipo, ?bool $lida, ?string $canal, ?string $periodo): array
    {
        $corte = $this->resolverPeriodo($periodo);
        $busca = $this->normalizar($busca);
        $tipo = $this->normalizar($tipo);
        $canal = $this->normalizar($canal);

        $items = NotificacaoHistorico::query()
            ->when($busca !== null, fn ($q) => $q->where(
                fn ($w) => $w->where('titulo', 'ilike', "%{$busca}%")->orWhere('mensagem', 'ilike', "%{$busca}%")
            ))
            ->when($tipo !== null, fn ($q) => $q->where('tipo', $tipo))
            ->when($lida !== null, fn ($q) => $q->where('lida', $lida))
            ->when($canal !== null, fn ($q) => $q->where('canal', $canal))
            ->when($corte !== null, fn ($q) => $q->where('criada_em', '>=', $corte))
            ->orderBy('criada_em', 'desc')
            ->get();

        return ['items' => $items, 'total' => $items->count()];
    }

    /**
     * Registro interno (listeners/schedulers): valida o link (D-6, 422 se inválido).
     */
    public function registrar(
        int $idUsuario,
        ?string $tipo,
        ?string $canal,
        string $titulo,
        ?string $mensagem,
        ?string $link,
        ?int $idReferencia = null,
        StatusEntrega $status = StatusEntrega::PENDENTE,
    ): Notificacao {
        if (trim($titulo) === '') {
            throw new \InvalidArgumentException('Título da notificação é obrigatório');
        }

        LinkValidator::assertValido($link);

        return Notificacao::create([
            'id_usuario' => $idUsuario,
            'tipo' => $tipo,
            'canal' => $canal,
            'titulo' => trim($titulo),
            'mensagem' => $mensagem,
            'link' => $link === null ? null : trim($link),
            'lida' => false,
            'criada_em' => now(),
            'status' => $status,
            'data_envio' => $status === StatusEntrega::ENVIADA ? now() : null,
            'id_referencia' => $idReferencia,
        ]);
    }

    /**
     * Revisão do Admin (D9/docs/07 §6): move p/ o histórico e some do ativo.
     * Ids ausentes no ativo são ignorados (revisão idempotente).
     */
    public function revisar(array $ids, int $idAdmin): int
    {
        $movidas = 0;

        DB::transaction(function () use ($ids, $idAdmin, &$movidas): void {
            foreach (array_unique(array_map('intval', $ids)) as $id) {
                $ativa = Notificacao::find($id);

                if ($ativa === null) {
                    continue;
                }

                NotificacaoHistorico::create([
                    'id_notificacao_original' => $ativa->getKey(),
                    'id_usuario' => $ativa->id_usuario,
                    'titulo' => $ativa->titulo,
                    'mensagem' => $ativa->mensagem,
                    'tipo' => $ativa->tipo,
                    'canal' => $ativa->canal,
                    'link' => $ativa->link,
                    'lida' => $ativa->lida,
                    'criada_em' => $ativa->criada_em,
                    'status' => $ativa->status,
                    'data_envio' => $ativa->data_envio,
                    'data_leitura' => $ativa->data_leitura,
                    'id_referencia' => $ativa->id_referencia,
                    'data_revisao_admin' => now(),
                    'id_admin_revisor' => $idAdmin,
                ]);

                $ativa->delete();
                $movidas++;
            }
        });

        return $movidas;
    }

    public function canalHabilitado(string $canal): bool
    {
        $linha = ConfiguracaoCanal::where('canal', strtoupper($canal))->first();

        return $linha !== null && (bool) $linha->habilitado;
    }

    public function preferenciaHabilitada(string $tipo, string $canal): bool
    {
        $linha = PreferenciaNotificacao::where('tipo', $tipo)->where('canal', $canal)->first();

        return $linha !== null ? (bool) $linha->habilitado : $this->preferenciaPadrao($tipo, $canal);
    }

    private function preferenciaPadrao(string $tipo, string $canal): bool
    {
        return ! ($tipo === 'sistema' && $canal === 'push');
    }

    private function resolverPeriodo(?string $periodo): ?\Carbon\CarbonImmutable
    {
        if ($periodo === null || trim($periodo) === '') {
            return null;
        }

        $valor = strtolower(trim($periodo));
        $agora = now()->toImmutable();

        if (str_ends_with($valor, 'd') && ctype_digit(substr($valor, 0, -1))) {
            return $agora->subDays((int) substr($valor, 0, -1));
        }

        if (str_ends_with($valor, 'h') && ctype_digit(substr($valor, 0, -1))) {
            return $agora->subHours((int) substr($valor, 0, -1));
        }

        throw new \InvalidArgumentException('Período inválido: use formatos como 24h ou 30d');
    }

    private function normalizar(?string $valor): ?string
    {
        if ($valor === null || trim($valor) === '') {
            return null;
        }

        return trim($valor);
    }
}
