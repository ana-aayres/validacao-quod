package br.com.fiap.validacaoquod.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import br.com.fiap.validacaoquod.model.FraudeModel;
import br.com.fiap.validacaoquod.repository.FraudeRepository;
import br.com.fiap.validacaoquod.util.ValidacaoFraude;
import br.com.fiap.validacaoquod.util.ValidacaoSelfie;

@RestController
@RequestMapping("/api/notificacoes/fraude")
public class FraudeController {

    @Autowired
    private FraudeRepository fraudeRepository;

    @PostMapping(consumes = {"multipart/form-data"}, produces = {"application/json"})
    public ResponseEntity<?> salvarFraude(
            @RequestParam("dados") String dadosJson, 
            @RequestParam("documento") MultipartFile file,
            @RequestParam("selfie1") MultipartFile selfieNeutra, 
            @RequestParam("selfie2") MultipartFile selfieSorrindo) {
        try {
            if (file.isEmpty() || selfieNeutra.isEmpty() || selfieSorrindo.isEmpty()) {
                return ResponseEntity.badRequest().body("Erro: Todas as imagens devem ser enviadas.");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            FraudeModel fraude = objectMapper.readValue(dadosJson, FraudeModel.class);

            Map<String, Object> fraudeDetalhes = new HashMap<>(); // Criando registro de fraude

            // Processamento dos metadados do documento
            try (InputStream input = file.getInputStream()) {
                Metadata metadata = ImageMetadataReader.readMetadata(input);
                Map<String, Object> documentoJson = new HashMap<>();

                for (Directory directory : metadata.getDirectories()) {
                    Map<String, Object> tags = new HashMap<>();
                    for (Tag tag : directory.getTags()) {
                        tags.put(tag.getTagName(), tag.getDescription());
                    }
                    documentoJson.put(directory.getName(), tags);
                }

                // Verifica fraude no documento
                String resultadoFraudeDocumento = ValidacaoFraude.verificarFraude(documentoJson);
                fraudeDetalhes.put("fraudeDocumento", resultadoFraudeDocumento);

                fraude.setDocumento(documentoJson);
            }

            // **Validação das selfies**
            Map<String, String> metadadosSelfie1 = ValidacaoSelfie.extrairMetadados(selfieNeutra);
            Map<String, String> metadadosSelfie2 = ValidacaoSelfie.extrairMetadados(selfieSorrindo);

            boolean valido = ValidacaoSelfie.validarMesmaDataELocalizacao(metadadosSelfie1, metadadosSelfie2);
            String motivoFraudeSelfie;

            if (!valido) {
                motivoFraudeSelfie = String.format(
                    "Fraude detectada nas selfies! Diferenças encontradas:\n" +
                    "- Data da selfie 1: %s\n" +
                    "- Data da selfie 2: %s\n" +
                    "- Localização da selfie 1 (Lat/Lon): %s, %s\n" +
                    "- Localização da selfie 2 (Lat/Lon): %s, %s",
                    metadadosSelfie1.getOrDefault("Date/Time Original", "Não encontrado"),
                    metadadosSelfie2.getOrDefault("Date/Time Original", "Não encontrado"),
                    metadadosSelfie1.getOrDefault("GPS Latitude", "Não encontrado"),
                    metadadosSelfie1.getOrDefault("GPS Longitude", "Não encontrado"),
                    metadadosSelfie2.getOrDefault("GPS Latitude", "Não encontrado"),
                    metadadosSelfie2.getOrDefault("GPS Longitude", "Não encontrado")
                );
            } else {
                motivoFraudeSelfie = "✅ Nenhuma anomalia detectada nas selfies.";
            }

            fraude.setSelfSeria(new HashMap<>(metadadosSelfie1));
            fraude.setSelfSorrindo(new HashMap<>(metadadosSelfie2));
            fraudeDetalhes.put("fraudeSelfie", motivoFraudeSelfie);

            fraude.setIndicativoFraude(fraudeDetalhes); // Adicionando o motivo da fraude ao banco

            // Salva no banco
            FraudeModel fraudeSalva = fraudeRepository.save(fraude);
            return ResponseEntity.ok(fraudeSalva);
            
        } catch (IOException e) {
            e.printStackTrace(); 
            return ResponseEntity.internalServerError().body("Erro ao processar a imagem: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.internalServerError().body("Erro inesperado: " + e.getMessage());
        }
    }

    @GetMapping
    public List<FraudeModel> getAllFraudes() {
        return fraudeRepository.findAll();
    }
}