# LibertyX Folder

Gerenciador de arquivos para Android no estilo do "Meus Arquivos" da Samsung, com SMB de primeira classe e pasta segura.

- **Início:** busca no aparelho, recentes, categorias (Imagens, Vídeos, Áudio, Documentos, Downloads, Instalação, Compactados), armazenamento, pasta segura e rede.
- **Pastas:** lista/grade, ordenação natural, ocultos, trilha de navegação, puxar para atualizar, busca na pasta e em subpastas.
- **Seleção:** mover, copiar (colar na pasta de destino), compartilhar, excluir, renomear, abrir com, detalhes, mover para/retirar da pasta segura.
- **SMB (smbj + dcerpc):** busca automática (mDNS + varredura da porta 445 + nome NetBIOS), lista de compartilhamentos, usuário/senha ou convidado, ler/gravar/renomear/mover/excluir, miniaturas, arquivos abertos em outros apps **sem baixar antes** (`SmbProvider`, `openProxyFileDescriptor`, com busca/seek).
- **Operações** em serviço de primeiro plano com progresso e cancelamento; nunca sobrescreve ("nome (1).ext").

## TV (Google TV e Fire Stick)

Mesmo código, dois APKs (`productFlavors`): `dist/LibertyXFolder-Celular.apk` e `dist/LibertyXFolder-TV.apk`. O de TV declara `leanback` obrigatório, banner e `LEANBACK_LAUNCHER`. Sem biblioteca nativa, então serve a ARM 32/64 (Fire Stick) e x86. Os dois têm o mesmo pacote: um substitui o outro. O `leanback` obrigatório só filtra na loja; `adb install` instala qualquer um em qualquer aparelho.

No controle remoto: realce de foco forte (`Tv.kt`), setas trocam de campo nos formulários, busca abre diálogo em vez de campo (focar campo abre o teclado), PIN aceita os números do controle e fica lado a lado com o teclado numérico, margem de segurança de 48 dp, sem "puxar para atualizar" (use Mais → Atualizar). Toque longo no botão central seleciona.

Fire TV sem a tela de "acesso a todos os arquivos": o app pede acesso às mídias e mostra o comando `adb shell appops set com.mauricio.libertyx.folder MANAGE_EXTERNAL_STORAGE allow`. SMB funciona sem permissão nenhuma ("Só usar servidores de rede").

## Pasta segura

PIN numérico ou senha com letras, digitado no teclado do sistema → PBKDF2-HMAC-SHA256 (150 mil iterações) → chave que embrulha uma chave mestra aleatória, embrulhada de novo por chave do Android Keystore. Arquivos em AES-256-GCM em blocos de 1 MiB (`VaultCrypto`, com detecção de alteração e de corte), nomes e pastas num índice cifrado, tudo na área privada do app.

**Configurações** (engrenagem no início): abrir, bloquear agora, alterar PIN/senha (confere o atual primeiro; permite trocar de PIN para senha), mostrar/ocultar no início (oculta: some do início e da opção "Mover para pasta segura" enquanto bloqueada; mudar exige o PIN), bloqueio automático (ao sair, 1, 5 ou 30 min), **apagar após 10 tentativas erradas** e **excluir pasta segura** (reconfirma o PIN). Espera crescente após 5 erros. Excluir ou esquecer o PIN apaga tudo e volta as opções ao padrão — não há recuperação, por projeto.

## Compilar

```bash
export JAVA_HOME=/DATA/Claudinho/toolchain/jdk-17.0.20.1+1
./gradlew testCelularDebugUnitTest assembleRelease   # APKs em dist/
```

## Armadilhas já pagas

- smbj como convidado: SMB 3 dá NullPointerException (usar 2.1) **e** assinatura precisa ficar desligada, senão o Samba recusa ("Bad SMB2 signature") e a conexão morre por tempo.
- smbj não lista compartilhamentos: é o RPC srvsvc do `dcerpc` (excluindo a versão antiga do smbj que ele traz).
- GCM num arquivo só não serve para vídeo: o Android segura tudo na memória até o fim. Por isso os blocos.
