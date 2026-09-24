package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fablab.producao.service.QrCodeService;
import org.junit.jupiter.api.Test;

class QrCodeServiceTest {

    private final QrCodeService service = new QrCodeService();

    @Test
    void geraPngValido() {
        byte[] png = service.gerarPng("fablab://projeto-mesa/1");
        assertTrue(png.length > 0);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 'P', png[1]);
        assertEquals((byte) 'N', png[2]);
        assertEquals((byte) 'G', png[3]);
    }

    @Test
    void geraPngComTamanhoCustomizado() {
        byte[] png = service.gerarPng("conteudo", 100);
        assertTrue(png.length > 0);
    }

    @Test
    void geraDataUri() {
        String dataUri = service.gerarDataUri("fablab://projeto-mesa/1");
        assertTrue(dataUri.startsWith("data:image/png;base64,"));
    }
}