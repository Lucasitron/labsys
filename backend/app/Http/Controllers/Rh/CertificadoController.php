<?php

namespace App\Http\Controllers\Rh;

use App\Enums\Rh\StatusSolicitacao;
use App\Enums\Rh\TipoCertificado;
use App\Http\Controllers\Controller;
use App\Http\Requests\Rh\DecisaoSolicitacaoRequest;
use App\Http\Requests\Rh\SolicitarCertificadoRequest;
use App\Http\Resources\Rh\CertificadoResource;
use App\Http\Resources\Rh\SolicitacaoResource;
use App\Services\Rh\CertificadoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Certificados — só HTTP. Aprovar/rejeitar são Admin (rota `can:admin`). */
class CertificadoController extends Controller
{
    use ResolvesRhPrincipal;

    public function __construct(private CertificadoService $certificados) {}

    public function solicitar(SolicitarCertificadoRequest $request): JsonResponse
    {

        $solicitacao = $this->certificados->solicitar([
            'tipoCertificado' => TipoCertificado::from($request->input('tipoCertificado')),
            'horasSolicitadas' => $request->input('horasSolicitadas'),
        ], $this->principal($request));

        return response()->json(
            (new SolicitacaoResource($solicitacao->load('funcionario.pessoa')))->toArray($request),
            201
        );
    }

    public function solicitacoes(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'in:PENDENTE,APROVADO,REJEITADO'],
        ]);

        $lista = $this->certificados->listarSolicitacoes(
            isset($filtros['status']) ? StatusSolicitacao::from($filtros['status']) : null,
            $this->principal($request),
        );

        return response()->json(collect($lista)->map(
            fn ($s) => (new SolicitacaoResource($s))->toArray($request)
        )->all());
    }

    public function aprovar(DecisaoSolicitacaoRequest $request, int $id): JsonResponse
    {

        $certificado = $this->certificados->aprovar($id, $request->input('observacao'), $this->principal($request));

        return response()->json(
            (new CertificadoResource($certificado, $this->consolidadas($certificado)))->toArray($request)
        );
    }

    public function rejeitar(DecisaoSolicitacaoRequest $request, int $id): JsonResponse
    {

        $solicitacao = $this->certificados->rejeitar($id, $request->input('observacao'), $this->principal($request));

        return response()->json((new SolicitacaoResource($solicitacao))->toArray($request));
    }

    public function emitidos(Request $request): JsonResponse
    {

        $lista = $this->certificados->listarEmitidos($this->principal($request));

        return response()->json(array_map(
            fn ($c) => (new CertificadoResource($c, $this->consolidadas($c)))->toArray($request),
            $lista,
        ));
    }

    public function emitido(Request $request, int $id): CertificadoResource
    {

        $certificado = $this->certificados->obterEmitido($id, $this->principal($request));

        return new CertificadoResource($certificado, $this->consolidadas($certificado));
    }

    /** @return list<array{idApontamento:int,horas:string}> */
    private function consolidadas(mixed $certificado): array
    {
        return array_map(
            fn ($h) => [
                'idApontamento' => (int) $h->id_apontamento,
                'horas' => number_format((float) $h->horas, 2, '.', ''),
            ],
            $this->certificados->consolidadasDe((int) $certificado->getKey()),
        );
    }
}
