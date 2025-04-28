package br.com.fiap.validacaoquod.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.fiap.validacaoquod.model.FraudeModel;

public interface FraudeRepository extends MongoRepository<FraudeModel, String> {
}

