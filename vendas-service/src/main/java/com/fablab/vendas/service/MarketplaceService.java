package com.fablab.vendas.service;

import com.fablab.vendas.dto.MarketplaceRequest;
import com.fablab.vendas.dto.MarketplaceResponse;
import com.fablab.vendas.dto.MarketplaceVendaEvent;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.RegistroMarketplace;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.RegistroMarketplaceRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro manual de vendas realizadas em marketplaces externos.
 */
@Service
public class MarketplaceService {

    private final RegistroMarketplaceRepository registroRepository;
    private final EncomendaRepository encomendaRepository;
    private final VendasEventPublisher eventPublisher;

    public MarketplaceService(RegistroMarketplaceRepository registroRepository,
                              EncomendaRepository encomendaRepository,
                              VendasEventPublisher eventPublisher) {
        this.registroRepository = registroRepository;
        this.encomendaRepository = encomendaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MarketplaceResponse registrarVenda(MarketplaceRequest request) {
        Encomenda encomenda = encomendaRepository.findById(request.idEncomenda())
                .orElseThrow(() -> new ResourceNotFoundException("Encomenda não encontrada: " + request.idEncomenda()));

        RegistroMarketplace registro = new RegistroMarketplace();
        registro.setEncomenda(encomenda);
        registro.setPlataforma(request.plataforma());
        registro.setCodigoExterno(request.codigoExterno());
        registro.setDataVenda(request.dataVenda() != null ? request.dataVenda() : LocalDate.now());
        registro.setValorTaxa(request.valorTaxa());
        registro = registroRepository.save(registro);

        eventPublisher.publishMarketplaceVenda(new MarketplaceVendaEvent(
                registro.getId(), encomenda.getId(), registro.getPlataforma(),
                registro.getDataVenda(), registro.getValorTaxa()));

        return MarketplaceResponse.of(registro);
    }
}