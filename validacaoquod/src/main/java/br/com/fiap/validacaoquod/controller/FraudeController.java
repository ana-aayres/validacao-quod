package br.com.fiap.validacaoquod.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.validacaoquod.model.FraudeModel;
import br.com.fiap.validacaoquod.repository.FraudeRepository;

@RestController
@RequestMapping("/api/notificacoes/fraude")
public class FraudeController {
    
    @Autowired
    private FraudeRepository fraudeRepository;

    @PostMapping
    public ResponseEntity<FraudeModel> salvarFraude(@RequestBody FraudeModel fraude) {
        FraudeModel fraudeSalva = fraudeRepository.save(fraude);
        return ResponseEntity.ok(fraudeSalva);

    }
    
    @GetMapping
    public List<FraudeModel> getAllFraudes() {
        return fraudeRepository.findAll();
    }

}
