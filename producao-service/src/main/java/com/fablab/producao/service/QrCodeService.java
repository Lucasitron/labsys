package com.fablab.producao.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Geração de QR Codes (ZXing) para os totens dos projetos de mesa. */
@Service
public class QrCodeService {

    private static final int TAMANHO_PADRAO = 250;

    /** Gera o QR Code em PNG. */
    public byte[] gerarPng(String conteudo) {
        return gerarPng(conteudo, TAMANHO_PADRAO);
    }

    /** Gera o QR Code em PNG com o tamanho informado (pixels). */
    public byte[] gerarPng(String conteudo, int tamanho) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    conteudo,
                    BarcodeFormat.QR_CODE,
                    tamanho,
                    tamanho,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name()));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Falha ao gerar o QR Code", ex);
        }
    }

    /** Gera o QR Code como data URI base64, pronto para uso em HTML/img. */
    public String gerarDataUri(String conteudo) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(gerarPng(conteudo));
    }
}