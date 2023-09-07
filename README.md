# O repositorio
Neste repositorio se encontra os scripts para fazer deploy da aplicação versão generica.

### Maquinas
  ### Criar o diretorio usando o comando baixo:
    sudo mkdir -p java/springboot/digital/nome-do-seu-servico
    Da permissão na pasta  do nome-do-seu-servico
    chmod -R 777 nome-do-seu-servico
  ### Instalar o java 11.
   sudo apt update<br>
   sudo apt install default-jre

### Com os seguintes passos:
 #### 1 -  Build: 
 Construção do projeto compilação. 
 #### 2 - Code Quality - Sonar: 
 Faz análise do códig.
 #### 3 - Quality Gate: 
 Verifica se a aplicação atingiu porcentagem definida no sonar/status.
 #### 4 - Build Docke: 
 Faz o build da imagem no docker compose.
 #### 5 - Publish Container DEV - 
 Publica no ambiente desejado.


# pipeline scripts
Este repositorio se encontra os scripts para a publicação dos projetos. Esses scripts tem o passo a passo do jenkins para fazer a públicação dos projetos.
#### Obervação: os scripts aqui depositado devem ter a extensão *.groove.

# pipeline file
O pipeline file contem os passos para necessario para a publicação do projeto.

#### Exemplos do passos de publicação  de um projeto.
![Passos para a publicaçõa de um projeto](https://miro.medium.com/v2/resize:fit:640/format:webp/1*SGuCtn2Gj_Q1fOg0MjBd9g.png)


# Uma visão do jenkins
![visão](https://www.cloudbees.com/sites/default/files/blog/pipeline-vis.png)

# Criando uma service no linux.
  
  ### craindo o arquivo digital-config-service.service
    sudo nano /etc/systemd/system/digital-config-service.service.
  
  ### conteúdo do arquivo
    [Unit]
        Description=Email service
        Requires=network.target remote-fs.target
        After=network.target remote-fs.target
    [Service]
        Type=simple
        User=ubuntu
        WorkingDirectory=/opt/apps/
        ExecStart=/usr/bin/java -jar /opt/apps/email-1.0.0.jar
        Restart=on-failure
        RestartSec=10
    [Install]
        WantedBy=multi-user.target
  #### Obervação: Esse service será chamado  pelo script o nome do arquivo tem que ser o mesmo do script.

  ### Comando para verificar o status do serviço.
      sudo systemctl daemon-reload
      sudo systemctl start digital-config-service.service
      sudo systemctl status digital-config-service.service
    
  
