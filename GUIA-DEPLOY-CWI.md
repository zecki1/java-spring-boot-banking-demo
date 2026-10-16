# Guia rápido: Deploy Java no Railway

## 1. Acesse Railway
- https://railway.app/
- Faça login com GitHub

## 2. Deploy do Projeto
- Clique "New Project" → "Deploy from GitHub repo"
- Selecione o repositório: `java-spring-boot-demo`
- Railway irá detectar automaticamente (pom.xml) e iniciar o build

## 3. Verificar Deploy
- Aguarde build (2-5 minutos)
- Railway gera URL automática: `https://java-spring-boot-demo-production.up.railway.app`
- Teste: `curl https://SEU-URL/actuator/health`

## 4. Integrar com Angular (ledger-bank)
- Abra `angular-ledger-bank/src/environments/environment.prod.ts`
- Atualize: `apiUrl: 'https://SEU-URL/api'`
- Re-deploy no Vercel

## 5. Demo para CWI
- Frontend + Backend Java funcionando
- Mencione Spring Boot, REST API, integração externa
- Use o Node.js em Vercel como "redundância" para explicar arquitetura
- Show: Transações, Contas, Taxa Selic (API BC), Câmbio

Repositório Java: https://github.com/zecki1/java-spring-boot-demo
Repositório Node: https://github.com/zecki1/node-express-demo-cwi
