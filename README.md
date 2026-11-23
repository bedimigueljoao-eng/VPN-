# Internet Vault (Android)

Aplicação Android de conectividade, gestão de VPN (`VpnService`) com servidores próprios e monitorização inteligente de internet móvel com foco nas operadoras de Angola (**Unitel Angola** e **Africell Angola**).

## Baixar a aplicação Android directamente pelo GitHub

Este repositório inclui um workflow automático (**GitHub Actions**) que compila o código-fonte e publica o ficheiro instalável `.apk` na secção **Releases** sempre que o código é enviado para o GitHub.

### Passos para transferir e instalar no telemóvel:
1. No GitHub, abra o separador **Releases** (na barra lateral direita do repositório) ou aceda a:
   `https://github.com/<SEU_UTILIZADOR>/<SEU_REPOSITORIO>/releases/latest`
2. Transfira o ficheiro **`Internet-Vault-Android.apk`**.
3. Abra o ficheiro `.apk` no seu telemóvel Android e confirme a instalação (autorize *"Instalar aplicações desconhecidas"* para o navegador ou gestor de ficheiros, se solicitado).
4. Abra o **Internet Vault**, conceda a permissão de notificação e aceite o pedido do sistema para criar o túnel VPN ao tocar em **LIGAR**.

> **Nota:** Se acabou de enviar o projecto para o GitHub pela primeira vez, aguarde cerca de 2 a 4 minutos no separador **Actions** até o workflow **`Build & Publish Internet Vault APK`** terminar com o ícone verde. O APK ficará disponível tanto em **Releases** como em **Actions → Artifacts**.

## Funcionalidades Principais
- **Painel Principal:** Botão central animado para ligar/desligar a VPN, IP do túnel (`10.84.0.2`), cronómetro de sessão, velocidade de download/upload em tempo real e indicadores de 3 pilares (VPN activa, servidor disponível e acesso efectivo à internet).
- **Módulo de Operadoras (Angola):** Detecção automática de rede e gestão dedicada para **Unitel Angola (`631-02`)** e **Africell Angola (`631-05`)**, fluxo de preparação em Wi-Fi para uso posterior em dados móveis e informação sobre conectividade patrocinada B2B (Zero-Rating).
- **Laboratório de Conectividade Unitel & Africell:**
  - **Testes Independentes por Operadora:** Matriz de conectividade de 9 verificações executada de forma independente para **Unitel (`631-02`)** e **Africell (`631-05`)** (rede móvel, dados móveis, internet pública, domínio oficial da operadora, servidor VPN configurado, serviços documentados/autorizados, DNS, validação Android e detecção de portal cativo).
  - **Comparação Antes vs Depois da VPN:** Captura lado a lado de IP público antes/depois, estado do túnel `tun0`, latência ao servidor e à internet, pacotes encaminhados e consumo de dados.
  - **Modo Sem Saldo / Sem Pacote:** Classificação rigorosa em 7 estados distintos sem classificar nenhuma ligação como gratuita sem evidência verificável e autorização oficial da operadora.
  - **Registo de Serviços Documentados:** Catálogo local persistente de serviços autorizados/documentados por operadora com fonte, data de verificação, condições e estado de autorização.
  - **Histórico & Exportação PDF/CSV:** Persistência local em Room com exportação nativa para **PDF** e **CSV** via Android SAF.
- **Servidores Próprios & Cofre Offline:** Adição, edição, teste real de latência TCP, importação/exportação em JSON e persistência local via Room.
- **Histórico & Diagnóstico:** Registo detalhado de sessões por operadora com gráfico comparativo e teste de diagnóstico completo.

