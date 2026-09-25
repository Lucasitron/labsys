import { describe, expect, it } from 'vitest';

// EST-035 · e2e — condicional (SKIP)
// Playwright NÃO está instalado (package.json não lista @playwright/test) e `npm test`
// só inclui `tests/unit/**`. Este arquivo documenta os fluxos a cobrir quando o e2e
// for habilitado — rodar com `npx playwright test frontend/e2e` (instalar antes).
//
// Fluxos planejados (mockup docs/mokups/estoque):
//   1. navegar → /estoque → redirect para /estoque/itens
//   2. itens: lista real (GET /estoque/itens), filtros categoria/idLocalizacao/baixo
//   3. itens/novo: criar item (POST 201) → toast + redirect lista
//   4. entradas/nova: criar entrada (POST 201) com ItemPicker + Fornecedor
//   5. saidas/nova: criar saída (POST 201) com tipoSaida CONSUMO/PERDA/AJUSTE/EMPRESTIMO
//   6. emprestimos: tab atrasados (GET /estoque/emprestimos/atrasados)
//   7. emprestimos/novo: criar empréstimo (searchPeople + POST 201)
//   8. fornecedores/novo: criar fornecedor (POST 201)
//   9. localizacoes/novo: criar localização (POST 201)
//  10. itens/[id]/editar: alterar item (PUT 200) e voltar ao detalhe
//  11. entradas/[id] e saidas/[id]: detalhes sem escrita
//  12. emprestimos/[id] e [id]/devolucao: registrar devolução (PUT sem body)
//
// data-testids esperados: it-*, ent-*, sai-*, emp-*, for-*, loc-*, bom-* (pt-BR).

describe.skip('estoque — e2e (requer Playwright; desabilitado)', () => {
	it('lista itens reais a partir de /estoque/itens', () => {
		expect(true).toBe(true);
	});

	it('cria item em itens/novo e retorna à lista', () => {
		expect(true).toBe(true);
	});

	it('cria entrada em entradas/nova', () => {
		expect(true).toBe(true);
	});

	it('cria saída em saidas/nova com tipoSaida', () => {
		expect(true).toBe(true);
	});

	it('exibe tab atrasados em emprestimos', () => {
		expect(true).toBe(true);
	});

	it('cria empréstimo em emprestimos/novo', () => {
		expect(true).toBe(true);
	});

	it('cria fornecedor em fornecedores/novo', () => {
		expect(true).toBe(true);
	});

	it('cria localização em localizacoes/novo', () => {
		expect(true).toBe(true);
	});

	it('edita item em itens/[id]/editar', () => {
		expect(true).toBe(true);
	});

	it('abre detalhes de entrada/saída sem escrita', () => {
		expect(true).toBe(true);
	});

	it('registra devolução em emprestimos/[id]/devolucao', () => {
		expect(true).toBe(true);
	});
});