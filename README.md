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
- **Ícone gerado por geometria** (`Scripts/icone.py`, precisa de `shapely resvg-py pillow`): uma **pasta de ouro com o X da marca vazado nela**. A primeira versão era o X do Player com uma pasta no lugar do play, e na tela inicial os dois viravam o mesmo borrão dourado — num ícone pequeno o que se enxerga é a mancha, não o detalhe do meio. Por isso a mancha aqui é **invertida**: o Player é escuro com um X de ouro, este é quase todo de ouro com o X aberto. O X é buraco de verdade (subtração de formas), então o mesmo desenho serve no adaptativo, no monocromático do Android 13, na marca dentro do app (`ic_marca`) e na faixa da TV. Rodar o script reescreve os `drawable/ic_launcher_*`, o banner da TV e `Scripts/play-512.png`.

## TV (Google TV e Fire Stick)

Mesmo código, dois APKs (`productFlavors`): `dist/LibertyXFolder-Celular.apk` e `dist/LibertyXFolder-TV.apk`. O que muda entre eles, além da entrada na tela inicial, são três chaves de `BuildConfig` definidas em `app/build.gradle.kts`:

| Chave | Celular | TV | O que faz |
|---|---|---|---|
| `PRO` | não | sim | 7 dias grátis e a rede como compra única (`pro/Pro.kt`, `pro/Loja.kt` só na TV); no celular tudo é grátis |
| `PASTA_SEGURA` | não | não | nenhum dos dois tem pasta segura; quem tinha uma acha em Configurações como esvaziá-la |
| `GUIA` | não | sim | o guia de uso de 10 páginas (`ui/Guia.kt`) na primeira abertura |

Há uma segunda dimensão de variante, a **loja** (`play` / `amazon`), e só a cobrança muda entre elas: a Google Play Billing na TV da Play (`src/tvPlay`, `tvPlayImplementation`), a Amazon Appstore SDK no Fire TV (`src/tvAmazon`, `tvAmazonImplementation`); no celular/tablet, grátis nas duas, `pro/Loja.kt` é um esboço com a mesma forma (`src/celular`). O de TV declara `leanback` obrigatório, banner e `LEANBACK_LAUNCHER`. Sem biblioteca nativa, então serve a ARM 32/64 (Fire Stick) e x86. Os dois têm o mesmo pacote: um substitui o outro, e na Play vão para a **mesma página**, na mesma versão (`./gradlew pacotesPlay` gera `dist/play/LibertyXFolder-Celular.aab` e `-TV.aab`). A Play entrega o pacote compatível de número mais alto: o celular só aceita o de celular (o de TV exige `leanback`), e na TV vence o de TV porque termina em 2 e o de celular em 1 — por isso os dois sobem sempre do mesmo `pacotesPlay`. O `leanback` obrigatório só filtra na loja; `adb install` instala qualquer um em qualquer aparelho.

No controle remoto: realce de foco forte (`Tv.kt`), setas trocam de campo nos formulários, busca abre diálogo em vez de campo (focar campo abre o teclado), PIN aceita os números do controle pelo teclado do sistema, margem de segurança de 48 dp, sem "puxar para atualizar" (use Mais → Atualizar). Toque longo no botão central seleciona.

O painel de progresso (copiar/mover/excluir) **some sozinho** 3,5 s depois de terminar bem: no controle remoto, descer até um "OK" que flutua sobre a lista é quase impossível, e um "Concluído" não tem nada a decidir. Só fica na tela o que precisa ser lido — erro ou cancelamento —, e aí, na TV, o painel chama o foco para si. Voltar também fecha o painel terminado, em qualquer aparelho.

Fire TV sem a tela de "acesso a todos os arquivos": o app pede acesso às mídias e mostra o comando `adb shell appops set com.mauricio.libertyx.folder MANAGE_EXTERNAL_STORAGE allow`. SMB funciona sem permissão nenhuma ("Só usar servidores de rede").

## Idiomas

O app segue o idioma do aparelho, como o LibertyX Player: **inglês é o padrão** (`values/`, e `resources.properties` declara isso), com **português** (`values-pt/`) e **espanhol** (`values-es/`); qualquer outro idioma cai no inglês. No Android 13+ a lista sai de `generateLocaleConfig`, e dá para escolher um idioma só para o app. O pacote da loja leva os três (`bundle.language.enableSplit = false`).

- Nas telas: `stringResource` / `pluralStringResource`. Fora delas (erros, notificação, títulos das operações): `Textos.get` / `Textos.plural`.
- Texto que muda com "PIN"/"senha" tem **duas strings** (`…_pin`, `…_senha`), e não interpolação — o artigo e a concordância mudam de idioma para idioma.
- Tamanhos (`Fmt.size`) usam a vírgula ou o ponto do idioma; datas vêm do sistema.
- **Texto novo entra nos três idiomas.** Testar: `adb shell cmd locale set-app-locales com.mauricio.libertyx.folder --locales es-ES` (e `--locales ""` para voltar ao do sistema).

## LibertyX Folder Pro (TV)

O modelo do LibertyX Player. Os arquivos da TV são grátis para sempre; a **rede** (servidores SMB, a busca automática, abrir e copiar da rede) vem liberada nos 7 primeiros dias e depois pede o Pro, compra única `libertyx_folder_pro`. O teste é contado pelo app (a Play não tem teste para compra única); a data vai no backup do Android — só ela, ver `src/tv/res/xml/backup_tv.xml` —, para reinstalar não recomeçar.

Terminado o teste, os computadores continuam aparecendo na tela inicial: o que trava é abrir, e quem tenta abrir cai na tela do Pro (`ui/ProScreen.kt`; o desvio fica no roteamento da `MainActivity`, então vale venha a pessoa por onde vier).

- `dist/` (compilado aqui): Pro **liberado** (`BuildConfig.LIBERADO`).
- `./gradlew pacotesPlay` → `dist/play/LibertyXFolder-{Celular,TV}.aab`, assinados com a chave de envio, Pro travado. (Não usar `bundleLoja`: ele gera também a variante Amazon.)
- `./gradlew pacotesAmazon` → `dist/amazon/LibertyXFolder-FireTV.apk` (Pro pela Amazon) e `-FireTablet.apk` (grátis), assinados com `keystore/folder-tv.jks`, Pro travado. Exige `app/src/tvAmazon/assets/AppstoreAuthenticationKey.pem` (a chave pública que a Amazon gera para o app); sem ela o build para. Passo a passo em `loja/AMAZON.md`.
- Testar a compra/o teste aqui: `-PlibertyxLoja` (trava) e `-PlibertyxDiasDeTeste=0` (teste vencido).
- Lançamento: `loja/PASSO-A-PASSO.md`, `loja/FICHA.md`, imagens em `loja/imagens/`, política em `PRIVACY.md`.

### O APK do GitHub (chave LibertyX)

Além da Play e da Amazon, a TV sai numa terceira loja, `github`
(`./gradlew assembleTvGithubRelease`): o mesmo app, com o Pro comprado pela
**chave LibertyX** — Bitcoin ou USDT/USDC pelo bot do Telegram
([@libertx_partition_bot](https://t.me/libertx_partition_bot?start=folder)). A
chave (produto 3, ou 9 do pacote) é conferida no aparelho, sem internet
(`pro/Chave.kt`), e colada pelo celular numa página que a própria TV serve em
`http://<ip-da-tv>:8787` enquanto o painel está aberto (`pro/AtivacaoNaRede.kt`).
Play e Amazon não têm nada disso: as duas proíbem outro meio de pagamento.

O CI (`.github/workflows/android.yml`) publica a cada push:
[`LibertyXFolder-TV.apk`](https://github.com/fxmx182/libertyx-folder-android/releases/latest/download/LibertyXFolder-TV.apk),
assinado com a `folder-tv.jks` (secret `FOLDER_TV_KEYSTORE_BASE64`).

## Guia de uso (TV)

`ui/Guia.kt`: dez páginas, cada uma com uma cena do próprio app encolhida e animada ao lado de um texto curto — controle remoto, tela inicial, pastas, seleção, copiar/mover, rede, assistir sem baixar, pesquisa e o Pro. ◀ ▶ mudam de página de qualquer lugar (os botões ficam em coluna para as setas laterais não serem deles); Voltar volta uma página. Aparece antes do pedido de permissão; flag `guia.visto.v1`. **Ao mudar uma ferramenta, mude a cena e o texto dela no guia.**

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
