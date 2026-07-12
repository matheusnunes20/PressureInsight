# PressureTestAnalyzer

Automatiza a análise de testes de pressão hidrostática (indústria de óleo e gás),
substituindo o processo manual de importar arquivos de sensores (Teksensor,
Additel) no Excel para montar gráficos e checar critérios de aceitação.

## Estrutura do repositório

```
PressureTestAnalyzer/
├── backend/     Java 21 + Spring Boot + Gradle
└── frontend/    React + Vite + Material UI
```

## Backend (`backend/`)

Camadas em `com.pressuretestanalyzer`:

| Pacote | Responsabilidade |
|---|---|
| `controller` | Endpoints REST |
| `service` | Casos de uso / regras de negócio |
| `repository` | Persistência (Spring Data JPA + SQLite) |
| `model` | Entidades de domínio |
| `dto` | Contratos de entrada/saída da API |
| `parser` | Leitura dos `.txt` exportados pelos sensores |
| `validation` | Validação de arquivo e de critério de aceitação |
| `chart` | Geração do gráfico Pressão x Tempo |
| `export` | Exportação em PNG/PDF |
| `config` | Configuração do Spring (CORS, datasource) |
| `exception` | Exceções de domínio e tratamento centralizado de erros |

Rodar localmente:

```
cd backend
./gradlew bootRun
```

O banco SQLite é criado em `backend/data/pressure-test-analyzer.db`.

## Frontend (`frontend/`)

```
src/
├── components/   componentes reutilizáveis
├── pages/        telas (Dashboard, Importar, Configurações)
├── services/     chamadas HTTP à API
├── hooks/        hooks customizados
├── types/        tipos TypeScript espelhando os DTOs do backend
└── theme/        tema Material UI
```

Rodar localmente:

```
cd frontend
npm install
cp .env.example .env
npm run dev
```

A API é esperada em `http://localhost:8080/api` (configurável via `VITE_API_BASE_URL`).

## Status

Apenas a estrutura do projeto está pronta. Os módulos (parser, validação,
cálculo de queda percentual, geração de gráfico, exportação, dashboard) serão
implementados de forma incremental.
