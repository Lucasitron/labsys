<?php

namespace App\Modules\Notification\Services;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Notification\Enums\StatusEntrega;
use App\Modules\Notification\Models\Notificacao;
use App\Modules\Notification\Models\NotificacaoHistorico;
use App\Modules\Rh\Contracts\RhContract;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Mail;

/**
 * Fan-in de entrega (único ponto consumido pelos 5 listeners + 2 schedulers).
 *
 * - `inapp` **sempre** (sem opt-out, docs/07 §8; nasce `ENVIADA`).
 * - `email`/`push` só com pref `(tipo,canal)` on; `email` exige ainda
 *   `configuracao EMAIL` on + endereço resolvido, e tenta `Mail::raw`
 *   fail-soft (`ENVIADA`, ou `PENDENTE` p/ retry — sem SMTP no ambiente).
 * - `push` ≡ persiste `ENVIADA` (polling no GET — sem infra push no MVP).
 * - WhatsApp = só seed off, zero código (docs/07 §2: fora do MVP).
 */
class NotificationDispatchService
{
    public function __construct(
        private NotificacaoService $inbox,
        private AuthContract $auth,
        private RhContract $rh,
    ) {}

    public function notificar(
        string $tipo,
        int $idUsuario,
        string $titulo,
        ?string $mensagem,
        ?int $idReferencia = null,
        ?string $email = null,
    ): void {
        $this->inbox->registrar($idUsuario, $tipo, 'inapp', $titulo, $mensagem, null, $idReferencia, StatusEntrega::ENVIADA);

        if ($this->inbox->preferenciaHabilitada($tipo, 'push')) {
            $this->inbox->registrar($idUsuario, $tipo, 'push', $titulo, $mensagem, null, $idReferencia, StatusEntrega::ENVIADA);
        }

        if ($email !== null && $this->inbox->preferenciaHabilitada($tipo, 'email') && $this->inbox->canalHabilitado('EMAIL')) {
            $this->enviarEmail($idUsuario, $tipo, $titulo, $mensagem, $idReferencia, $email);
        }
    }

    /** Broadcast p/ Admins (lista vazia = warn + zero rows, nunca 500). */
    public function notificarAdmins(
        string $tipo,
        string $titulo,
        ?string $mensagem,
        ?int $idReferencia = null,
    ): void {
        $admins = $this->auth->adminIds();

        if ($admins === []) {
            Log::warning("Notificação '{$tipo}' sem destinatários: nenhum Admin ativo");
        }

        foreach ($admins as $idAdmin) {
            $this->notificar($tipo, $idAdmin, $titulo, $mensagem, $idReferencia, $this->resolverEmail($idAdmin));
        }
    }

    /** Funcionário via `RhContract` (null quando sem vínculo/e-mail). */
    public function notificarFuncionario(
        string $tipo,
        int $idFuncionario,
        string $titulo,
        ?string $mensagem,
        ?int $idReferencia = null,
    ): void {
        $destino = $this->rh->dadosDestinatario($idFuncionario);

        if ($destino === null) {
            Log::warning("Notificação '{$tipo}' ignorada: funcionário {$idFuncionario} sem destinatário");

            return;
        }

        $this->notificar($tipo, $destino['idUsuario'], $titulo, $mensagem, $idReferencia, $destino['email']);
    }

    /** E-mail do destinatário via `RhContract` (null sem vínculo/e-mail). */
    public function emailDe(int $idUsuarioOuFuncionario): ?string
    {
        return $this->resolverEmail($idUsuarioOuFuncionario);
    }

    /** Reenvio de e-mails `PENDENTE` (scheduler a cada 30 min); retorna as reenviadas. */
    public function reenviarPendentes(): int
    {
        $reenviadas = 0;

        foreach (Notificacao::where('canal', 'email')->where('status', StatusEntrega::PENDENTE)->get() as $pendente) {
            $email = $this->resolverEmail((int) $pendente->id_usuario);

            if ($email === null || ! $this->inbox->canalHabilitado('EMAIL')) {
                continue;
            }

            try {
                $this->dispararEmail($email, (string) $pendente->titulo, $pendente->mensagem);
            } catch (\Throwable $e) {
                Log::warning("Reenvio de notificação {$pendente->getKey()} falhou: {$e->getMessage()}");

                continue;
            }

            $pendente->status = StatusEntrega::ENVIADA;
            $pendente->data_envio = now();
            $pendente->save();
            $reenviadas++;
        }

        return $reenviadas;
    }

    /** Expurgo do histórico com revisão há >90 dias; retorna as removidas. */
    public function expurgarHistorico(): int
    {
        return NotificacaoHistorico::where('data_revisao_admin', '<', now()->subDays(90))->delete();
    }

    private function enviarEmail(
        int $idUsuario,
        string $tipo,
        string $titulo,
        ?string $mensagem,
        ?int $idReferencia,
        string $email,
    ): void {
        try {
            $this->dispararEmail($email, $titulo, $mensagem);
        } catch (\Throwable $e) {
            Log::warning("Envio de e-mail p/ notificação '{$tipo}' falhou (PENDENTE p/ retry): {$e->getMessage()}");
            $this->inbox->registrar($idUsuario, $tipo, 'email', $titulo, $mensagem, null, $idReferencia, StatusEntrega::PENDENTE);

            return;
        }

        Log::info("Notificação '{$tipo}' enviada por e-mail p/ {$email}");
        $this->inbox->registrar($idUsuario, $tipo, 'email', $titulo, $mensagem, null, $idReferencia, StatusEntrega::ENVIADA);
    }

    private function dispararEmail(string $email, string $titulo, ?string $mensagem): void
    {
        Mail::raw((string) $mensagem, function ($correio) use ($email, $titulo): void {
            $correio->to($email)->subject($titulo);
        });
    }

    private function resolverEmail(int $idUsuarioOuFuncionario): ?string
    {
        return $this->rh->dadosDestinatario($idUsuarioOuFuncionario)['email'] ?? null;
    }
}
