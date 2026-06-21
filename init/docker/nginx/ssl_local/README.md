# 🔒 Configuração de Certificados Locais (TLS/SSL) e Domínios

Para rodar este projeto localmente utilizando HTTPS no Nginx e conexões seguras no MongoDB (exigência do Mongo 8.0+), é necessário gerar certificados autoassinados, importá-los e configurar os domínios locais no seu sistema.

Siga o passo a passo abaixo para configurar o seu ambiente.

## Pré-requisitos
Você precisará da ferramenta **mkcert** instalada na sua máquina. 
* **Windows (via Chocolatey):** `choco install mkcert`
* **macOS (via Homebrew):** `brew install mkcert`
* **Linux:** Siga as instruções do repositório oficial do mkcert.

---

## Passo 1: Instalar a CA local
Abra o seu terminal (como Administrador) e instale a Autoridade Certificadora local do mkcert na sua máquina:
```powershell
mkcert -install
```

## Passo 2: Gerar os Certificados de Domínio
Navegue até a pasta onde os certificados ficarão armazenados (`nginx/ssl_local`) e gere os certificados para os domínios locais utilizados pelos contêineres:

```powershell
cd nginx/ssl_local/

# Isso gerará os arquivos tedros.test+4.pem e tedros.test+4-key.pem
mkcert tedros.test localhost 127.0.0.1 ::1 mongodb
```

## Passo 3: Criar o Certificado Unificado do MongoDB
O MongoDB exige que o certificado e a chave privada estejam no mesmo arquivo. Ainda dentro da pasta `nginx/ssl_local`, execute o comando abaixo para concatenar os arquivos criados no passo anterior:

**No PowerShell (Windows):**
```powershell
Get-Content tedros.test+4.pem, tedros.test+4-key.pem | Set-Content mongodb.pem
```

**No Bash (Linux/macOS/Git Bash):**
```bash
cat tedros.test+4.pem tedros.test+4-key.pem > mongodb.pem
```

## Passo 4: Copiar a CA Root para o Docker
O contêiner do MongoDB precisa conhecer a CA raiz do seu `mkcert` para validar a cadeia de confiança.
Descubra onde a sua CA raiz está salva rodando:
```powershell
mkcert -CAROOT
```
Vá até o diretório retornado por este comando, copie o arquivo **`rootCA.pem`** e cole-o dentro da pasta `nginx/ssl_local/` do projeto.

---

## Passo 5: Importar o Certificado no Cacerts do Java
Para que a aplicação Java confie na conexão TLS do MongoDB, você precisa importar o `rootCA.pem` gerado pelo `mkcert` para dentro do *keystore* (cacerts) do seu JDK.

Abra o **PowerShell como Administrador** e adapte os caminhos abaixo conforme a sua instalação do JDK:

```powershell
# 1. Defina o caminho do rootCA.pem que você copiou no passo anterior (ou use o diretório original do mkcert)
$caFile = "C:\Caminho\Para\Seu\Projeto\nginx\ssl_local\rootCA.pem"

# 2. Defina os caminhos do seu JDK (Substitua pelo seu diretório real do Java)
$keytool = "C:\Caminho\Para\Seu\jdk-17\bin\keytool.exe"
$cacerts = "C:\Caminho\Para\Seu\jdk-17\lib\security\cacerts"

# 3. Importe o certificado (A senha padrão do cacerts é 'changeit')
& $keytool -import -trustcacerts -keystore $cacerts -storepass changeit -alias mkcert-local -file $caFile -noprompt
```

*Nota: Se o comando for bem-sucedido, você verá a mensagem "Certificate was added to keystore".*

---

## Passo 6: Atualizar o arquivo hosts do Windows
Para que os domínios locais do projeto funcionem corretamente no seu navegador e sejam roteados para os contêineres, você precisa mapeá-los para o seu endereço local (`127.0.0.1`).

1. Abra o Bloco de Notas (ou seu editor de texto preferido) como **Administrador**.
2. Vá em **Arquivo > Abrir** e navegue até: `C:\Windows\System32\drivers\etc\hosts` *(não esqueça de mudar o filtro de arquivos de "*.txt" para "Todos os arquivos" para enxergá-lo)*.
3. Adicione as seguintes linhas ao final do arquivo e salve:

```text
127.0.0.1       tedros.test
127.0.0.1       www.tedros.test
127.0.0.1       h2db.tedros.test
```

---

## Passo 7: Subir os Contêineres
Com tudo configurado e os arquivos presentes na pasta `nginx/ssl_local` (`mongodb.pem`, `rootCA.pem`, `tedros.test+4.pem` e `tedros.test+4-key.pem`), você já pode subir o ambiente:

```powershell
# Volte para a raiz do docker-compose
cd ../../
docker-compose up -d
```