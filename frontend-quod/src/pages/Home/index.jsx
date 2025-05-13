import './style.css';
import { Cadastrar } from '../../services/api';

function Home() {
  const handleCadastro = async (event) => {
    event.preventDefault(); // Evita o reload da página

    console.log('Botão "Cadastrar" foi clicado!');

    // Captura os valores dos inputs
    const dados = {
      nome: event.target.nome.value,
      cpf: event.target.cpf.value,
    };

    const documento = event.target.documento.files[0];
    const selfie1 = event.target.selfieSeria.files[0];
    const selfie2 = event.target.selfieSorrindo.files[0];

    console.log('Dados capturados:', dados);
    console.log('Documento:', documento);
    console.log('Selfie séria:', selfie1);
    console.log('Selfie sorrindo:', selfie2);

    // Chama a API
    try {
      const response = await Cadastrar(dados, documento, selfie1, selfie2);
      console.log('Resposta do servidor:', response);
    } catch (error) {
      console.error('Erro ao chamar Cadastrar:', error);
    }
  };

  return (
    <>
      <div className='container'>
        <form onSubmit={handleCadastro}>
          <h1>Cadastro de usuário</h1>
          <input placeholder='Nome completo' name='nome' type='text' required />
          <input placeholder='CPF' name='cpf' type='text' required />
          <input name='documento' type='file' required />
          <input name='fingerprint' type='file' />
          <input name='selfieSeria' type='file' required />
          <input name='selfieSorrindo' type='file' required />
          <button type='submit'>Cadastrar</button>
        </form>
      </div>
    </>
  );
}

export default Home;