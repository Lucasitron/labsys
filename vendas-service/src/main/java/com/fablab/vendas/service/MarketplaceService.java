package com.fablab.vendas.service;

import com.fablab.vendas.dto.ClienteDtos.ClienteResponse;
import com.fablab.vendas.dto.EncomendaDtos.EncomendaResponse;
import com.fablab.vendas.dto.MarketplaceDtos.MarketplaceListaResponse;
import com.fablab.vendas.dto.MarketplaceDtos.MarketplaceRequest;
import com.fablab.vendas.dto.MarketplaceDtos.MarketplaceResponse;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.RegistroMarketplace;
import com.fablab.vendas.exception.ConflitoException;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.ItemOrcamentoRepository;
import com.fablab.vendas.repository.RegistroMarketplaceRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro manual de vendas externas (taxa em R$; líquido = bruto − taxa,
 * calculado no backend).
 */
@Service
public class MarketplaceService {

    private final RegistroMarketplaceRepository registroRepository;
    private final EncomendaRepository encomendaRepository;
    private final ClienteRepository clienteRepository;
    private final ItemOrcamentoRepository itemRepository;

    public MarketplaceService(RegistroMarketplaceRepository registroRepository,
                              EncomendaRepository encomendaRepository,
                              ClienteRepository clienteRepository,
                              ItemOrcamentoRepository itemRepository) {
        this.registroRepository = registroRepository;
        this.encomendaRepository = encomendaRepository;
        this.clienteRepository = clienteRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public MarketplaceResponse registrar(MarketplaceRequest request) {
        Encomenda encomenda = encomendaRepository.findById(request.encomendaId())
                .orElseThrow(() -> new ResourceNotFoundException("Encomenda não encontrada"));
        if (registroRepository.existsByPlataformaAndCodigoExterno(
                request.plataforma().trim(), request.codigoExterno().trim())) {
            throw new ConflitoException("Pedido já registrado para esta plataforma");
        }
        RegistroMarketplace registro = new RegistroMarketplace();
        registro.setIdEncomenda(encomenda.getId());
        registro.setPlataforma(request.plataforma().trim());
        registro.setCodigoExterno(request.codigoExterno().trim());
        registro.setDataVenda(request.dataVenda());
        registro.setValorTaxa(request.valorTaxa());
        return paraResponse(registroRepository.save(registro));
    }

    @Transactional(readOnly = true)
    public MarketplaceListaResponse listar(Long encomendaId, String plataforma) {
        List<RegistroMarketplace> registros = registroRepository.findAll();
        if (encomendaId != null) {
            registros = registros.stream().filter(r -> r.getIdEncomenda().equals(encomendaId)).toList();
        }
        if (plataforma != null && !plataforma.isBlank()) {
            registros = registros.stream()
                    .filter(r -> r.getPlataforma().equalsIgnoreCase(plataforma.trim())).toList();
        }
        List<MarketplaceResponse> respostas = registros.stream().map(this::paraResponse).toList();
        BigDecimal bruto = respostas.stream().map(MarketplaceResponse::valorBruto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxas = respostas.stream().map(MarketplaceResponse::valorTaxa)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> totais = new LinkedHashMap<>();
        totais.put("bruto", bruto);
        totais.put("taxas", taxas);
        totais.put("liquido", bruto.subtract(taxas));
        return new MarketplaceListaResponse(respostas, totais);
    }

    /** Líquido = valor final da encomenda − taxa (em R$). */
    public BigDecimal liquido(BigDecimal valorBruto, BigDecimal valorTaxa) {
        return valorBruto.subtract(valorTaxa);
    }

    private MarketplaceResponse paraResponse(RegistroMarketplace registro) {
        Encomenda encomenda = encomendaRepository.findById(registro.getIdEncomenda()).orElse(null);
        BigDecimal bruto = encomenda == null ? BigDecimal.ZERO : encomenda.getValorFinal();
        String clienteNome = encomenda == null ? "—" : clienteRepository.findById(encomenda.getIdCliente())
                .map(Cliente::getNomeRazaoSocial).orElse("—");
        return new MarketplaceResponse(registro.getId(), registro.getIdEncomenda(),
                "EN-" + registro.getIdEncomenda(), registro.getPlataforma(), registro.getCodigoExterno(),
                clienteNome, bruto, registro.getValorTaxa(), liquido(bruto, registro.getValorTaxa()),
                registro.getDataVenda());
    }
}
