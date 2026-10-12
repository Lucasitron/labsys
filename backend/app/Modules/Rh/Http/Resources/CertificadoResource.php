<?php

namespace App\Modules\Rh\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class CertificadoResource extends JsonResource
{
    /** @param list<array{idApontamento:int,horas:string}> $consolidadas */
    public function __construct($resource, private array $consolidadas = [])
    {
        parent::__construct($resource);
    }

    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'idCertificado' => (int) $this->id,
            'idSolicitacao' => (int) $this->id_solicitacao,
            'idFuncionario' => (int) $this->id_funcionario,
            'nomeFuncionario' => $this->funcionario?->pessoa?->nome_completo,
            'tipoCertificado' => $this->tipo_certificado->value,
            'horasCertificadas' => number_format((float) $this->horas_certificadas, 2, '.', ''),
            'dataEmissao' => $this->data_emissao->toIso8601String(),
            'codigoVerificacao' => (string) $this->codigo_verificacao,
            'horas' => $this->consolidadas,
        ];
    }
}
