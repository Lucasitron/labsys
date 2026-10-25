<?php

namespace App\Modules\Auth\Services;

use App\Modules\Auth\Models\ParametroSistema;
use App\Shared\Exceptions\ConfiguracaoInvalidaException;

/**
 * Parâmetros globais do sistema (KV auth.parametro_sistema). Só persiste
 * identidade + cadências; alimenta Produção (cadência 5S) por leitura.
 */
class SistemaService
{
    public const K_NOME = 'identidade.nomeFablab';

    public const K_LOGO = 'identidade.logo';

    public const K_CAD_CHECKLIST = 'cadencia.checklist5S';

    public const K_CAD_AUDITORIA = 'cadencia.auditoria5S';

    private const DEFAULTS = [
        self::K_NOME => 'FabLab IFPR — Curitiba',
        self::K_LOGO => '',
        self::K_CAD_CHECKLIST => 'Semanal',
        self::K_CAD_AUDITORIA => 'Mensal',
    ];

    /** Cadências aceitas (mockup cfg-sistema: Semanal/Quinzenal/Mensal). */
    private const CADENCIAS = ['SEMANAL', 'QUINZENAL', 'MENSAL'];

    public function __construct(private TokenIntegracaoService $tokens) {}

    /** @return array{identidade:array{nomeFablab:string,logo:string},cadenciaChecklist5S:string,cadenciaAuditoria5S:?string,tokens:list<array>} */
    public function obter(): array
    {
        $this->ensureDefaults();

        $auditoria = $this->valor(self::K_CAD_AUDITORIA);

        return [
            'identidade' => [
                'nomeFablab' => $this->valor(self::K_NOME),
                'logo' => $this->valor(self::K_LOGO),
            ],
            'cadenciaChecklist5S' => $this->valor(self::K_CAD_CHECKLIST),
            'cadenciaAuditoria5S' => blank($auditoria) ? null : $auditoria,
            'tokens' => $this->tokens->listarAtivos(),
        ];
    }

    /**
     * @param  array{identidade?:?array{nomeFablab?:?string,logo?:?string},cadenciaChecklist5S?:?string,cadenciaAuditoria5S?:?string}  $data
     * @return array{identidade:array{nomeFablab:string,logo:string},cadenciaChecklist5S:string,cadenciaAuditoria5S:?string,tokens:list<array>}
     */
    public function atualizar(array $data): array
    {
        $this->ensureDefaults();

        $identidade = $data['identidade'] ?? null;
        if (! is_array($identidade)) {
            throw new ConfiguracaoInvalidaException('Identidade é obrigatória');
        }

        $nome = trim((string) ($identidade['nomeFablab'] ?? ''));
        if ($nome === '') {
            throw new ConfiguracaoInvalidaException('Nome do laboratório é obrigatório');
        }
        if (mb_strlen($nome) > 255) {
            throw new ConfiguracaoInvalidaException('Nome do laboratório deve ter no máximo 255 caracteres');
        }

        $logo = trim((string) ($identidade['logo'] ?? ''));
        if (mb_strlen($logo) > 2048) {
            throw new ConfiguracaoInvalidaException('Logo deve ter no máximo 2048 caracteres');
        }

        $checklist = $this->normalizarCadencia(
            $data['cadenciaChecklist5S'] ?? null,
            'Cadência do checklist 5S é obrigatória',
            'Cadência do checklist 5S inválida: use Semanal, Quinzenal ou Mensal'
        );

        $auditoria = null;
        if (! blank($data['cadenciaAuditoria5S'] ?? null)) {
            $auditoria = $this->normalizarCadencia(
                $data['cadenciaAuditoria5S'],
                null,
                'Cadência da auditoria 5S inválida: use Semanal, Quinzenal ou Mensal'
            );
        }

        $this->salvar(self::K_NOME, $nome);
        $this->salvar(self::K_LOGO, $logo);
        $this->salvar(self::K_CAD_CHECKLIST, $checklist);
        $this->salvar(self::K_CAD_AUDITORIA, $auditoria ?? '');

        return $this->obter();
    }

    private function valor(string $chave): string
    {
        return ParametroSistema::find($chave)?->valor ?? self::DEFAULTS[$chave];
    }

    private function ensureDefaults(): void
    {
        foreach (self::DEFAULTS as $chave => $valor) {
            ParametroSistema::firstOrCreate(['chave' => $chave], ['valor' => $valor]);
        }
    }

    private function salvar(string $chave, string $valor): void
    {
        ParametroSistema::updateOrCreate(['chave' => $chave], ['valor' => $valor]);
    }

    private function normalizarCadencia(?string $raw, ?string $msgObrigatorio, string $msgInvalida): string
    {
        if (blank($raw)) {
            throw new ConfiguracaoInvalidaException($msgObrigatorio ?? $msgInvalida);
        }

        $normalized = mb_strtoupper(trim($raw));

        if (! in_array($normalized, self::CADENCIAS, true)) {
            throw new ConfiguracaoInvalidaException($msgInvalida);
        }

        return ucfirst(mb_strtolower($normalized));
    }
}
