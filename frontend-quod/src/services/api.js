import axios from 'axios';

export const Cadastrar = async (dados, documento, selfie1, selfie2, fingerprint) => {
  const formData = new FormData();
  
  formData.append('dados', JSON.stringify(dados));
  formData.append('documento', documento);  // Enviar o arquivo diretamente
  formData.append('selfie1', selfie1);
  formData.append('selfie2', selfie2);
  formData.append('fingerprint', fingerprint);


  try {
    const response = await axios.post('http://localhost:8080/api/notificacoes/fraude', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    console.log('Resposta do servidor:', response.data);
  } catch (error) {
    console.error('Erro ao salvar fraude:', error);
  }
};



