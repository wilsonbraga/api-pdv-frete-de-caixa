package com.pdv.modules.venda.service;

import com.pdv.modules.venda.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class NumeracaoVendaService {

    private final VendaRepository vendaRepository;

    // Cache de sequenciais por prefixo para evitar query a cada venda
    private final ConcurrentHashMap<String, AtomicInteger> sequenciais = new ConcurrentHashMap<>();

    /**
     * Gera o número da venda no formato: TENANTPREFIX-PDVPREFIX-000001
     * Ex: LOJA01-CX01-000123
     * Executa em transação separada para garantir isolamento.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String gerarNumero(UUID tenantId, String nomePdv) {
        // Pega os primeiros 8 chars do UUID (antes do primeiro '-') para ter margem
        String uuidPart = tenantId.toString().replace("-", "").substring(0, 8).toUpperCase();
        String prefixoTenant = normalizarPrefixo(uuidPart);
        String prefixoPdv    = normalizarPrefixo(nomePdv);
        String prefixo       = prefixoTenant + "-" + prefixoPdv + "-";

        AtomicInteger seq = sequenciais.computeIfAbsent(prefixo, p -> {
            int maxAtual = vendaRepository.findMaxSequencial(tenantId, p);
            return new AtomicInteger(maxAtual);
        });

        int proximo = seq.incrementAndGet();
        return prefixo + String.format("%06d", proximo);
    }

    private String normalizarPrefixo(String valor) {
        if (valor == null || valor.isBlank()) {
            return "PDV";
        }
        // Remove tudo que não for letra ou número, uppercase
        String limpo = valor.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (limpo.isEmpty()) {
            return "PDV";
        }
        // Limita a 6 chars usando o tamanho da string JÁ LIMPA
        return limpo.substring(0, Math.min(limpo.length(), 6));
    }
}
