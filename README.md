# Arquivos

Gerenciador de arquivos para Android no estilo do "Meus Arquivos" da Samsung, com SMB de primeira classe e pasta segura.

- **Início:** busca no aparelho, recentes, categorias (Imagens, Vídeos, Áudio, Documentos, Downloads, Instalação, Compactados), armazenamento, pasta segura e rede.
- **Pastas:** lista/grade, ordenação natural, ocultos, trilha de navegação, puxar para atualizar, busca na pasta e em subpastas.
- **Seleção:** mover, copiar (colar na pasta de destino), compartilhar, excluir, renomear, abrir com, detalhes, mover para/retirar da pasta segura.
- **SMB (smbj + dcerpc):** busca automática (mDNS + varredura da porta 445 + nome NetBIOS), lista de compartilhamentos, usuário/senha ou convidado, ler/gravar/renomear/mover/excluir, miniaturas, arquivos abertos em outros apps **sem baixar antes** (`SmbProvider`, `openProxyFileDescriptor`, com busca/seek).
- **Operações** em serviço de primeiro plano com progresso e cancelamento; nunca sobrescreve ("nome (1).ext").

## Pasta segura

PIN → PBKDF2-HMAC-SHA256 (150 mil iterações) → chave que embrulha uma chave mestra aleatória, embrulhada de novo por chave do Android Keystore. Arquivos em AES-256-GCM em blocos de 1 MiB (`VaultCrypto`, com detecção de alteração e de corte), nomes e pastas num índice cifrado, tudo na área privada do app. Espera crescente após 5 PINs errados; bloqueio automático após 1 min fora do app. Esqueceu o PIN = apagar a pasta (não há recuperação, por projeto).

## Compilar

```bash
export JAVA_HOME=/DATA/Claudinho/toolchain/jdk-17.0.20.1+1
./gradlew testDebugUnitTest assembleRelease   # APK em dist/Arquivos.apk
```

## Armadilhas já pagas

- smbj como convidado: SMB 3 dá NullPointerException (usar 2.1) **e** assinatura precisa ficar desligada, senão o Samba recusa ("Bad SMB2 signature") e a conexão morre por tempo.
- smbj não lista compartilhamentos: é o RPC srvsvc do `dcerpc` (excluindo a versão antiga do smbj que ele traz).
- GCM num arquivo só não serve para vídeo: o Android segura tudo na memória até o fim. Por isso os blocos.
