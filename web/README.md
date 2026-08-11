# TripNext Web

Versão web responsiva do planejador de viagens.

## Executar

```powershell
cd web
npm install
npm run dev
```

Abra `http://localhost:5173`.

## Build de produção

```powershell
npm run build
```

Os arquivos finais ficam em `web/dist`.

## Funcionalidades

- criação e edição do plano de viagem, com destino, período e viajantes;
- roteiro por dia com transporte, hospedagem, passeios e deslocamentos;
- horário, duração, endereço, reserva, link e observações por item;
- custos previstos totais, por pessoa e por categoria;
- checklist interativo para a preparação da viagem;
- layout desktop e mobile;
- persistência local via `localStorage`.
