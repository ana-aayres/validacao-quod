package br.com.fiap.validacaoquod.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
            String resultadoFraudeDocumento = "Nenhuma fraude detectada.";
            String motivoFraudeSelfie = "Nenhuma fraude detectada.";

            // Upload para S3 e obtenção das URIs
            String uriDocumento = s3Service.uploadFile(documento, "documentos/" + documento.getOriginalFilename());
            String uriSelfieNeutra = s3Service.uploadFile(selfieNeutra, "selfies/seria/" + selfieNeutra.getOriginalFilename());
            String uriSelfieSorrindo = s3Service.uploadFile(selfieSorrindo, "selfies/sorrindo/" + selfieSorrindo.getOriginalFilename());

            // Processamento dos metadados do documento
            Map<String, Object> documentoJson = extrairMetadadosDocumento(documento);
            documentoJson.put("uri", uriDocumento);
            resultadoFraudeDocumento = ValidacaoFraude.verificarFraude(documentoJson);
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

                if (!mesmaData) {
                    fraudeSelfieMsg.append("- Motivo: As selfies não foram tiradas no mesmo dia.\n");
                }
                if (!mesmaLocalizacao) {
                    fraudeSelfieMsg.append("- Motivo: As selfies não foram tiradas no mesmo local.\n");
                }

                motivoFraudeSelfie = fraudeSelfieMsg.toString();
            }

            fraudeDetalhes.put("fraudeSelfie", motivoFraudeSelfie);

            // Configuração final antes de salvar no banco
            fraude.setSelfSeria(new HashMap<>(metadadosSelfie1));
            fraude.setSelfSorrindo(new HashMap<>(metadadosSelfie2));
            

            // Validação final e envio de alerta
            String errosFraude = ValidacaoFraude.validarMensagemFraude(fraudeDetalhes, resultadoFraudeDocumento);
            if (!errosFraude.isEmpty()) {
                
                //chamar ferramenta de alerta
                // System.out.println("🚨 Fraude detectada! Enviando alerta...");
                // System.out.println(errosFraude);
                fraudeDetalhes.put("status", "NOK");
            } else {
                fraudeDetalhes.put("status", "OK");
                
            }

            fraude.setIndicativoFraude(fraudeDetalhes);

            // Salvar no banco
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