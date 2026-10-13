# PediuPatrao

Sistema de pedidos para pizzaria feito com Spring Boot 3, Thymeleaf, Spring Security e MongoDB.

## Como rodar

### Requisitos

- Java 21 ou superior
- Uma conta no [MongoDB Atlas](https://www.mongodb.com/atlas)

### 1. Criar o banco no MongoDB Atlas

1. Crie um cluster no MongoDB Atlas (o plano gratuito M0 serve).
2. Em **Database Access**, crie um usuário de banco com permissão de leitura e escrita (`USUARIO` / `SENHA`).
3. Em **Network Access**, libere o seu IP atual. Sem isso a conexão falha com erro de TLS (`internal_error`) ou timeout.
4. Em **Database > Connect > Drivers**, copie a string de conexão do seu cluster.

### 2. Criar o `application-local.properties`

Crie o arquivo `src/main/resources/application-local.properties` com a sua URI:

```properties
spring.data.mongodb.uri=mongodb+srv://USUARIO:SENHA@SEU-CLUSTER.mongodb.net/pizzaria?appName=NOME-DO-APP
```

Esse arquivo contém credenciais e está no `.gitignore`: **nunca** o versione.

Se a senha tiver caracteres especiais (`@`, `:`, `/`, `%`), codifique-os em formato URL (por exemplo, `@` vira `%40`).

### 3. Ativar o perfil `local`

- **IntelliJ:** *Run > Edit Configurations* > selecione a configuração da aplicação > campo **Active profiles** = `local`.
- **Terminal:**

  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
  ```

  No Windows (PowerShell): `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"`

### Alternativa: variável de ambiente

Sem o perfil `local`, a aplicação lê a variável `MONGODB_URI`. Se ela não existir, tenta `mongodb://localhost:27017/pizzaria`.

### Acesso

Com a aplicação no ar, acesse `http://localhost:8080`. Na primeira execução é criado automaticamente o usuário `admin`.
