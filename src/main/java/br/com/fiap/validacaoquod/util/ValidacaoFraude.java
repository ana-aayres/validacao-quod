package br.com.fiap.validacaoquod.util;

import java.util.Map;

public class ValidacaoFraude {

    public static String verificarFraude(Map<String, Object> documento) {
        // Obtém os dados da imagem dentro dos documento
        Map<String, Object> pngIhdr = (Map<String, Object>) documento.get("PNG-IHDR");

        if (pngIhdr == null) {
            return "Nenhum metadado encontrado para análise.";
        }

        // Pega valores específicos
        String compressionType = (String) pngIhdr.get("Compression Type");
        String filterMethod = (String) pngIhdr.get("Filter Method");

        // Define padrões suspeitos
        boolean compressaoSuspeita = compressionType != null && !compressionType.equals("Deflate");
        boolean filtroSuspeito = filterMethod != null && !filterMethod.equals("Adaptive");

        // Retorna alerta de possível fraude
        if (compressaoSuspeita || filtroSuspeito) {
            return "⚠️ Suspeita de fraude detectada! Compression Type: " + compressionType + 
                   ", Filter Method: " + filterMethod;
        }

        return "✅ Nenhuma anomalia detectada no documento.";
    }
}
