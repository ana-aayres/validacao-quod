package br.com.fiap.validacaoquod.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

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
            return "Suspeita de fraude detectada! Compression Type: " + compressionType + 
                   ", Filter Method: " + filterMethod;
        }

        return "Nenhuma fraude detectada.";
    }

    public static String validarMensagemFraude(Map<String, Object> fraudeDetalhes, String resultadoFraudeDocumento) {
        StringBuilder errosFraude = new StringBuilder();

        // Verifica fraude nas selfies
        String fraudeSelfieMsg = (String) fraudeDetalhes.get("fraudeSelfie");
        if (!"Nenhuma fraude detectada.".equals(fraudeSelfieMsg)) {
            errosFraude.append("Erro nas selfies: ").append(fraudeSelfieMsg).append("\n");
        }

        // Verifica fraude no documento
        if (!"Nenhuma fraude detectada.".equals(resultadoFraudeDocumento)) {
            errosFraude.append("Erro no documento: ").append(resultadoFraudeDocumento).append("\n");
        }

        return errosFraude.toString().trim();
    }

        private Map<String, Object> extrairMetadadosDocumento(MultipartFile file) {
        Map<String, Object> documentoJson = new HashMap<>();

        try (InputStream input = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(input);

            for (Directory directory : metadata.getDirectories()) {
                Map<String, Object> tags = new HashMap<>();
                for (Tag tag : directory.getTags()) {
                    tags.put(tag.getTagName(), tag.getDescription());
                }
                documentoJson.put(directory.getName(), tags);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return documentoJson;
    }

}
