# LibertyX Folder

Gerenciador de arquivos para Android no estilo do "Meus Arquivos" da Samsung, com SMB de primeira classe e pasta segura.

- **Início:** busca no aparelho, recentes, categorias (Imagens, Vídeos, Áudio, Documentos, Downloads, Instalação, Compactados), armazenamento, pasta segura e rede.
- **Pastas:** lista/grade, ordenação natural, ocultos, trilha de navegação, puxar para atualizar, busca na pasta e em subpastas.
- **Seleção:** mover, copiar (colar na pasta de destino), compartilhar, excluir, renomear, abrir com, detalhes, mover para/retirar da pasta segura.
- **SMB (smbj + dcerpc):** busca automática (mDNS + varredura da porta 445 + nome NetBIOS), lista de compartilhamentos, usuário/senha ou convidado, ler/gravar/renomear/mover/excluir, miniaturas, arquivos abertos em outros apps **sem baixar antes** (`SmbProvider`, `openProxyFileDescriptor`, com busca/seek).
- **Operações** em serviço de primeiro plano com progresso e cancelamento; nunca sobrescreve ("nome (1).ext").

## Visual

A mesma linguagem do LibertyX Player, e a mesma família de marca: **ouro sobre preto quente**, superfícies de vidro (branco a 5,5% com a borda a 11% — é a diferença entre os dois que dá a impressão de espessura), cantos de 22 dp, **Manrope** em todos os estilos do Material e títulos de seção pequenos, em caixa alta e espaçados. Tudo em `ui/Theme.kt` (`Lx`, `Modifier.vidro()`, `Modifier.brilhoDeFundo()`).

- **Escuro sempre**, e não só à noite: ouro sobre fundo claro vira mostarda. Não há mais `values-night` — o tema do aparelho não muda a cara do app.
- O **brilho de fundo** (um ouro fraco no alto à esquerda, um azul frio no rodapé) é desenhado uma vez na raiz da `MainActivity`; as telas são transparentes e todas ganham a mesma profundidade. Por isso a faixa da barra de status é um degradê, e não um bloco chapado.
- **Cor com função:** cada tipo de arquivo tem a sua (`Mime.color`), escolhida para fundo escuro. Pasta é a cor da marca. Numa lista quase só de pastas o selo cheio virava uma parede de ouro, então o da pasta é só uma insinuação — e na grade a placa dela é de vidro, com só o desenho dourado.
- **Ícone gerado por geometria** (`Scripts/icone.py`, precisa de `shapely resvg-py pillow`): as duas fitas de ouro cruzadas em X da família, trançadas nos cruzamentos à vista, com uma **pasta** no lugar do play do Player. O vão em volta dela é um buraco de verdade (subtração de formas), então o mesmo desenho serve no ícone adaptativo, no monocromático do Android 13, na marca dentro do app (`ic_marca`) e na faixa da TV. Rodar o script reescreve os `drawable/ic_launcher_*`, o banner da TV e `Scripts/play-512.png`.

## TV (Google TV e Fire Stick)

Mesmo código, dois APKs (`productFlavors`): `dist/LibertyXFolder-Celular.apk` e `dist/LibertyXFolder-TV.apk`. O de TV declara `leanback` obrigatório, banner e `LEANBACK_LAUNCHER`. Sem biblioteca nativa, então serve a ARM 32/64 (Fire Stick) e x86. Os dois têm o mesmo pacote: um substitui o outro. O `leanback` obrigatório só filtra na loja; `adb install` instala qualquer um em qualquer aparelho.

No controle remoto: realce de foco forte (`Tv.kt`), setas trocam de campo nos formulários, busca abre diálogo em vez de campo (focar campo abre o teclado), PIN aceita os números do controle pelo teclado do sistema, margem de segurança de 48 dp, sem "puxar para atualizar" (use Mais → Atualizar). Toque longo no botão central seleciona.

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
