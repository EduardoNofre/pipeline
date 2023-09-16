## Support & Feedback
Este projeto é mantido por Eduardo Nofre. Por favor, entenda que não poderemos fornecer suporte individual por e-mail. 
Acredito também que a ajuda é muito mais valiosa se for partilhada publicamente, para que mais pessoas possam beneficiar dela.

## O repositorio
Neste repositorio se encontra os scripts para fazer deploy da aplicação versão generica.

<h1 align="center">
    Para cada maquinas AWS criar
</h1>

  ### Criar o diretorio usando o comando baixo:
    - sudo mkdir -p java/springboot/digital/nome-do-seu-servico da permissão na pasta  do nome-do-seu-servico
    - chmod -R 777 nome-do-seu-servico
  ### Instalar o java 11.
   - sudo apt update
   - sudo apt install default-jre
  ### Instalar sshpass.
   - sudo apt update<br>
   - sudo apt-get install sshpass

## Criando uma service no linux.
  - sudo nano /etc/systemd/system/nome-da-sua-service.service.
  
## conteúdo do arquivo service
[Unit]
Description= serviço digital-config-service
After=network.target

[Service]<br>
  SuccessExitStatus=143<br>
  User=ubuntu<br>
  Type=simple<br>
  Restart=always<br>
  RestartSec=1<br>
  StartLimitInterval=0<br>
  WorkingDirectory=/java/springboot/digital/digital-config-service/<br>
  ExecStart=/usr/bin/java -jar /java/springboot/digital/digital-config-service/digital-config-service.jar<br>
  User=ubuntu<br>
[Install]<br>
  WantedBy=multi-user.target<br>
    
## Obervação: 
  - Esse service será chamado  pelo script o nome do arquivo tem que ser o mesmo do script.

## Comando utilizado para verificar o service linux.
  - sudo systemctl daemon-reload
  - sudo systemctl stop digital-config-service.service
  - sudo systemctl start digital-config-service.service
  - sudo systemctl status digital-config-service.service

## Com os seguintes passos:
 #### 1 -  Build: 
 - Construção do projeto compilação. 
 #### 2 - Code Quality - Sonar: 
 - Faz análise do códig.
 #### 3 - Quality Gate: 
 - Verifica se a aplicação atingiu porcentagem definida no sonar/status.
 #### 4 - Build Docke: 
 - Faz o build da imagem no docker compose.
 #### 5 - Publish Container 
 - Publica no ambiente desejado.
 - 
## pipeline scripts
Este repositorio se encontra os scripts para a publicação dos projetos. Esses scripts tem o passo a passo do jenkins para fazer a públicação dos projetos.
#### Obervação: os scripts aqui depositado devem ter a extensão *.groove.

# pipeline file
O pipeline file contem os passos para necessario para a publicação do projeto.

### Exemplos do passos de publicação  de um projeto.
![Passos para a publicaçõa de um projeto](https://miro.medium.com/v2/resize:fit:640/format:webp/1*SGuCtn2Gj_Q1fOg0MjBd9g.png)


# Uma visão do jenkins
![visão](https://www.cloudbees.com/sites/default/files/blog/pipeline-vis.png)

    
  
