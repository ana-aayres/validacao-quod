package br.com.fiap.validacaoquod.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "fraudes")
public class FraudeModel {
    @Id
    private String id;
    private String nome;
    private String cpf;
    private Map<String, Object> documento;
    private Map<String, Object> indicativoFraude;
    private Map<String, Object> selfSeria;
    private Map<String, Object> selfSorrindo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public Map<String, Object> getDocumento() {
        return documento;
    }

    public void setDocumento(Map<String, Object> documento) {
        this.documento = documento;
    }

    public Map<String, Object> getIndicativoFraude() {
        return indicativoFraude;
    }

    public void setIndicativoFraude(Map<String, Object> indicativoFraude) {
        this.indicativoFraude = indicativoFraude;
    }

    public Map<String, Object> getSelfSeria() {
        return selfSeria;
    }

    public void setSelfSeria(Map<String, Object> selfSeria) {
        this.selfSeria = selfSeria;
    }

    public Map<String, Object> getSelfSorrindo() {
        return selfSorrindo;
    }

    public void setSelfSorrindo(Map<String, Object> selfSorrindo) {
        this.selfSorrindo = selfSorrindo;
    }


    



    

 
}