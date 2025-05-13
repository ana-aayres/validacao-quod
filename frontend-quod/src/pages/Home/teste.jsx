import './style.css';
import { useState, useRef } from 'react';
import { Cadastrar } from '../../services/api';

function Home() {
  const [selfieSeria, setSelfieSeria] = useState(null);
  const [selfieSorrindo, setSelfieSorrindo] = useState(null);
  const [modoCaptura, setModoCaptura] = useState('seria');
  const [cadastroSucesso, setCadastroSucesso] = useState(false);
  const videoRef = useRef(null);
  const canvasRef = useRef(null);

  const abrirCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      videoRef.current.srcObject = stream;
    } catch (error) {
      console.error('Erro ao acessar a câmera:', error);
    }
  };

  const tirarFoto = () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    const context = canvas.getContext('2d');

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    const fotoData = canvas.toDataURL('image/png');

    if (modoCaptura === 'seria') {
      setSelfieSeria(fotoData);
      setModoCaptura('sorrindo');
    } else {
      setSelfieSorrindo(fotoData);
    }
  };

  // Função para converter Base64 em File
  const dataURLtoFile = (dataUrl, filename) => {
    const arr = dataUrl.split(',');
    const mime = arr[0].match(/:(.*?);/)[1];
    const bstr = atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new File([u8arr], filename, { type: mime });
  };

  const handleCadastro = async (event) => {
    event.preventDefault();

    const dados = {
      nome: event.target.nome.value,
      cpf: event.target.cpf.value,
    };

    const documento = event.target.documento.files[0];
    const fingerprint = event.target.fingerprint.files[0];

    // Converter selfies Base64 para File antes de enviar à API
    const selfieSeriaFile = selfieSeria ? dataURLtoFile(selfieSeria, "selfieSeria.png") : null;
    const selfieSorrindoFile = selfieSorrindo ? dataURLtoFile(selfieSorrindo, "selfieSorrindo.png") : null;

    try {
      await Cadastrar(dados, documento, fingerprint, selfieSeriaFile, selfieSorrindoFile);
      setCadastroSucesso(true); // Exibir pop-up de sucesso
    } catch (error) {
      console.error('Erro ao chamar Cadastrar:', error);
      alert('Erro ao cadastrar. Verifique sua conexão ou tente novamente.');
    }
  };

  return (
    <>
      <div className='container'>
        <form onSubmit={handleCadastro}>
          <h1>Cadastro de usuário</h1>
          <input placeholder='Nome completo' name='nome' type='text' required />
          <input placeholder='CPF' name='cpf' type='text' required />
          <p>Inserir documento</p>
          <input name='documento' type='file' required />
          <p>Inserir impressão digital</p>
          <input name='fingerprint' type='file' required />
          <button type='button' onClick={abrirCamera}>Abrir Câmera</button>
          <video ref={videoRef} autoPlay />
          <canvas ref={canvasRef} style={{ display: 'none' }} />
          
          {modoCaptura === 'seria' ? (
            <button type='button' onClick={tirarFoto}>Capturar Selfie Séria</button>
          ) : (
            <button type='button' onClick={tirarFoto}>Capturar Selfie Sorrindo</button>
          )}
          
          {selfieSeria && (
            <div className='foto'>
              <h3>Selfie Séria</h3>
              <img src={selfieSeria} alt="Selfie Séria" />
            </div>
          )}
          
          {selfieSorrindo && (
            <div className='foto'>
              <h3>Selfie Sorrindo</h3>
              <img src={selfieSorrindo} alt="Selfie Sorrindo" />
            </div>
          )}

          <button type='submit'>Cadastrar</button>
        </form>

        {cadastroSucesso && (
          <div className="modal">
            <div className="modal-content">
              <h2>Cadastro realizado com sucesso! 🎉</h2>
              <p>Seus dados foram enviados corretamente.</p>
              <button onClick={() => setCadastroSucesso(false)}>Fechar</button>
            </div>
          </div>
        )}
      </div>
    </>
  );
}

export default Home;