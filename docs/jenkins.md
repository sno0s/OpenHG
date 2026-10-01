# Jenkins do OpenHG

O job `OpenHG` usa o `jenkinsfile` da branch `main`.

Plugins necessários: Pipeline, Git, JUnit, Email Extension, HTML Publisher,
Coverage e Warnings Next Generation. Pipeline Graph View fornece a visualização
das etapas, sem configuração adicional no Jenkinsfile.

O build publica:

- resultados JUnit e relatório HTML **Testes OpenHG**;
- cobertura JaCoCo de linhas, branches e métodos;
- avisos do compilador Java, incluindo depreciações e operações unchecked;
- JAR com SQLite e ZIP do resource pack, com fingerprints.

JaCoCo 0.8.14 suporta Java 25. O relatório é gerado após `test`, em
`hardcoregames/build/reports/jacoco/test/`. Não há um percentual mínimo que
bloqueie builds; os dados iniciais servem para identificar lacunas de testes.
Warnings registra avisos do compilador, não uma análise Checkstyle/SpotBugs.

Para validar sem publicar no Minecraft, use **Build with Parameters** e
desmarque `DEPLOY`. Desmarque `NOTIFY_EMAIL` para não enviar e-mail nessa execução.
Ambos permanecem habilitados por padrão, preservando o fluxo existente.
O deploy atual substitui o JAR e remove os YAMLs do diretório do plugin;
esses arquivos são regenerados no próximo boot. Ele não reinicia o servidor.

A instalação de plugins sozinha não gera relatórios: é preciso executar um novo
build com esta versão do pipeline e do Gradle. O SMTP existente é usado pelo
Email Extension.
