package com.contractops.api.document.service

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

/**
 * Serviço de OCR (stub para Fase 2).
 * Futuramente integrado com Tika, Google Document AI, ou Agente de IA.
 */
@Service
@Profile("local")
class OcrService {

    fun extractText(filePath: String, mimeType: String?): String {
        // Stub avançado: em produção faria OCR real
        return when {
            mimeType?.contains("pdf", true) == true -> "[OCR] Texto extraído de PDF: $filePath (stub)"
            mimeType?.contains("image", true) == true -> "[OCR] Texto extraído de imagem: $filePath (stub)"
            else -> "[OCR] Conteúdo textual simulado do arquivo: $filePath"
        }
    }
}