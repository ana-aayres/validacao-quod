package br.com.fiap.validacaoquod.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fiap.validacaoquod.model.FraudeModel;
import br.com.fiap.validacaoquod.repository.FraudeRepository;
import br.com.fiap.validacaoquod.service.S3Service;
import br.com.fiap.validacaoquod.util.ValidacaoFraude;
import br.com.fiap.validacaoquod.util.ValidacaoSelfie;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/notificacoes/fraude")
public class FraudeController {

    @Autowired
    private FraudeRepository fraudeRepository;

    @Autowired
    private S3Service s3Service;

    @PostMapping(consumes = {"multipart/form-data"}, produces = {"application/json"})
    public ResponseEntity<?> salvarFraude(
            @RequestParam("dados") String dadosJson,  
            @RequestParam("documento") MultipartFile documento,
            @RequestParam("selfie1") MultipartFile selfieNeutra,  
            @RequestParam("selfie2") MultipartFile selfieSorrindo) {

        try {
            if (documento.isEmpty() || selfieNeutra.isEmpty() || selfieSorrindo.isEmpty()) {
                return ResponseEntity.badRequest().body("Erro: Todas as imagens devem ser enviadas.");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            FraudeModel fraude = objectMapper.readValue(dadosJson, FraudeModel.class);
            Map<String, Object> fraudeDetalhes = new HashMap<>();

            // Upload para S3
            String uriDocumento = s3Service.uploadFile(documento, "documentos/" + documento.getOriginalFilename());
            String uriSelfieNeutra = s3Service.uploadFile(selfieNeutra, "selfies/seria/" + selfieNeutra.getOriginalFilename());
            String uriSelfieSorrindo = s3Service.uploadFile(selfieSorrindo, "selfies/sorrindo/" + selfieSorrindo.getOriginalFilename());

            // Processamento dos metadados do documento
            Map<String, Object> documentoJson = extrairMetadados(documento);
            documentoJson.put("uri", uriDocumento);
            String resultadoFraudeDocumento = ValidacaoFraude.verificarFraude(documentoJson);
            fraudeDetalhes.put("fraudeDocumento", resultadoFraudeDocumento);
            fraude.setDocumento(documentoJson);

            // Extração e preenchimento dos metadados das selfies
            Map<String, String> metadadosSelfie1 = ValidacaoSelfie.extrairMetadados(selfieNeutra);
            Map<String, String> metadadosSelfie2 = ValidacaoSelfie.extrairMetadados(selfieSorrindo);

            metadadosSelfie1 = ValidacaoSelfie.preencherMetadados(metadadosSelfie1);
            metadadosSelfie2 = ValidacaoSelfie.preencherMetadados(metadadosSelfie2);

            // Validação das selfies
            boolean mesmaData = ValidacaoSelfie.validarMesmoDia(metadadosSelfie1, metadadosSelfie2);
            boolean mesmaLocalizacao = ValidacaoSelfie.validarMesmaLocalizacao(metadadosSelfie1, metadadosSelfie2);

            metadadosSelfie1.put("uri", uriSelfieNeutra);
            metadadosSelfie2.put("uri", uriSelfieSorrindo);

            if (!mesmaData || !mesmaLocalizacao) {
                StringBuilder fraudeSelfieMsg = new StringBuilder("Fraude detectada nas selfies!\n");
                if (!mesmaData) fraudeSelfieMsg.append("- As selfies não foram tiradas no mesmo dia.\n");
                if (!mesmaLocalizacao) fraudeSelfieMsg.append("- As selfies não foram tiradas no mesmo local.\n");

                fraudeDetalhes.put("fraudeSelfie", fraudeSelfieMsg.toString());
            } else {
                fraudeDetalhes.put("fraudeSelfie", "Nenhuma fraude detectada.");
            }

            // Validação final e envio de alerta ao Beeceptor
            String errosFraude = ValidacaoFraude.validarMensagemFraude(fraudeDetalhes, resultadoFraudeDocumento);
            if (!errosFraude.isEmpty()) {
                fraudeDetalhes.put("status", "NOK");
                
            } else {
                fraudeDetalhes.put("status", "OK");
            }
            
            fraudeDetalhes.put("nome", fraude.getNome());
            fraudeDetalhes.put("cpf", fraude.getCpf());
            enviarAlertaBeeceptor(fraudeDetalhes);
            fraude.setIndicativoFraude(fraudeDetalhes);
            FraudeModel fraudeSalva = fraudeRepository.save(fraude);
            return ResponseEntity.ok(fraudeSalva);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erro ao processar a imagem: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro inesperado: " + e.getMessage());
        }
    }

    @GetMapping
    public List<FraudeModel> getAllFraudes() {
        return fraudeRepository.findAll();
    }

    private Map<String, Object> extrairMetadados(MultipartFile file) {
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

    private void enviarAlertaBeeceptor(Map<String, Object> fraudeDetalhes) {
        try {
            URL url = new URL("https://validacaofraudequod.free.beeceptor.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = new ObjectMapper().writeValueAsString(fraudeDetalhes);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Resposta do Beeceptor: " + responseCode);
            connection.disconnect();
        } catch (Exception e) {
            System.err.println("Erro ao enviar alerta de fraude: " + e.getMessage());
        }
    }
}