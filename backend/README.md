# BikeShop Backend

API REST (Spring Boot 3 / Java 21) da plataforma BikeShop. Ver [plan.md](../specs/001-bike-shop-ecommerce/plan.md) para arquitetura completa.

## Rodando localmente

1. Suba a infraestrutura: `docker compose -f ../infra/docker-compose.yml up -d`
2. Copie `.env.example` para `.env` (ou exporte as variáveis) e ajuste se necessário
3. Se o wrapper do Maven (`mvnw`) ainda não existir neste diretório, gere-o uma vez (requer Maven instalado): `mvn -N wrapper:wrapper -Dmaven=3.9.9`
4. Rode a aplicação: `./mvnw spring-boot:run` (ou `mvn spring-boot:run` sem o wrapper)

API disponível em `http://localhost:8081` (porta 8081, não 8080, para não conflitar com outros serviços locais), documentação em `http://localhost:8081/swagger-ui.html`.

## Testes

```
./mvnw test
```
