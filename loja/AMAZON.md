# Amazon Appstore — LibertyX Folder (Fire TV e tablet Fire)

A Amazon Appstore hoje só existe nos aparelhos da própria Amazon — Fire TV e
tablets Fire (a dos celulares Android comuns fechou em 20/08/2025). O app vai
para lá **com o mesmo pacote** `com.mauricio.libertyx.files`, em dois APKs na
mesma página:

| APK | Aparelho | Pro |
|---|---|---|
| `dist/amazon/LibertyXFolder-FireTV.apk` | Fire TV (Fire OS) | 7 dias grátis, depois compra única pela **Amazon** |
| `dist/amazon/LibertyXFolder-FireTablet.apk` | tablet Fire | grátis, tudo liberado (como o celular na Play) |

Gerar: `./gradlew pacotesAmazon`. Assinados com `keystore/folder-tv.jks`, Pro
travado. **Precisa de `app/src/tvAmazon/assets/AppstoreAuthenticationKey.pem`**
— a chave pública que a Amazon gera para o app; sem ela a compra não funciona,
e por isso o build para com o aviso "Falta …pem".

- `minSdk 26`: entram Fire OS 7 (Android 9) e Fire OS 8 (Android 11). Fire
  OS 6 (Android 7.1, Fire TV Stick de 2ª geração e anteriores) fica de fora.
- **Vega OS** (o sistema novo da Amazon, sem Android — Fire TV Stick 4K
  Select) não roda app Android: não marcar esses aparelhos.
- O código da compra fica em `app/src/tvAmazon` (Appstore SDK 3.0.9). Os textos
  do Pro que citam a loja ("conta Amazon", "Amazon Appstore") também.

## Estado no console (2026-10-19)

- Conta criada; pagamento e entrevistas fiscais (royalties/W-8BEN e Canadá)
  completas; a de "serviço" (anúncios) fica incompleta de propósito.
- App `LibertyX Folder` (amzn1.devportal.mobileapp.87887f7cb1764c7691ce2048971600e7),
  categoria Productivity/Utilities › File Management, suporte
  libertyx.suporte@gmail.com, idiomas en/pt/es.
- APK1 FireTV 4196482 → só os 80 Fire TV com Fire OS (Vega OS fica de fora
  sozinho); APK2 FireTablet 4196481 → os 12 tablets Fire compatíveis. A
  Amazon tinha jogado tablets e "Automotive" no APK1 (número maior) — corrigido
  à mão em *Edit supported devices*. DRM: No nos dois.
- 18+, todos os países, classificação: tudo "None/No", não coleta dados,
  política de privacidade do repositório.
- Grátis; textos en-US, pt-BR, es-ES (ordenador/mando) e es-MX
  (computadora/control); imagens: ícones, Fire TV (ícone, fundo, 6 capturas
  `tv-en-*`), tablet (4 capturas `imagens/amazon/tablet-en-*`, tiradas no AVD
  viper-tablet com arquivos de exemplo), destaque 1024×500.
- IAP `libertyx_folder_pro` (Entitlement) "Ready to Submit": US$ 4.99 base
  (Japão ¥787, Brasil R$ 25,28, Europa € 4,44…).
- Instruções para o revisor preenchidas.
- **Falta (com o dono):** marcar *Export compliance*, *Submit IAP* e
  *Submit App*.

## O que só o dono da conta faz

1. Conta em **https://developer.amazon.com** (grátis), perfil de desenvolvedor.
2. **Payments**: banco e **Tax Interview** — pessoa física residente no Japão,
   não americana: formulário **W-8BEN** (o tratado EUA–Japão costuma zerar a
   retenção sobre royalties; o próprio formulário calcula).

## No console (dá para fazer junto, com a conta logada)

1. **Add New App › Android**: nome `LibertyX Folder`, categoria
   *Utilities* (ou *Productivity*), idioma padrão English (US).
2. **APK Files**: copiar a chave pública ("View public key" /
   *AppstoreAuthenticationKey.pem*) para
   `app/src/tvAmazon/assets/AppstoreAuthenticationKey.pem`, rodar
   `./gradlew pacotesAmazon` e subir os dois APKs.
   - FireTV.apk: deixar só os **Fire TV com Fire OS** marcados.
   - FireTablet.apk: deixar só os **tablets Fire** (desmarcar Fire TV).
   - "Apply Amazon DRM?": **No** (o app é grátis; a compra é que é protegida).
3. **In-App Items › Add Single IAP › Entitlement**:
   - SKU: `libertyx_folder_pro` (tem de ser exatamente este)
   - Título: `LibertyX Folder Pro` · preço base **US$ 4.99** (a Amazon converte
     para os outros países)
   - Descrição: ver abaixo · ícones: `imagens/amazon/icone-114.png` e
     `icone-512.png`
4. **Availability & Pricing**: app **grátis**, em todos os países.
5. **Description**: textos abaixo (en-US padrão, pt-BR e es-ES/es-MX).
6. **Images & Multimedia**:
   - Ícones: `imagens/amazon/icone-512.png` e `icone-114.png`
   - Fire TV: ícone `imagens/amazon/firetv-icone-1280x720.png` (arte dentro da
     área segura de 882×448) e fundo `imagens/amazon/firetv-fundo-1920x1080.jpg`
   - Capturas 1920×1080: `imagens/tv-en-*.jpg` (e `tv-pt-*`, `tv-es-*` nas
     traduções). As de tablet: tirar do app de celular quando o console pedir.
7. **Content Rating**: questionário — sem violência, sexo, apostas, etc.;
   "coleta dados?" **não**; tem compra no app: **sim**.
8. Política de privacidade:
   `https://github.com/fxmx182/libertyx-folder-android/blob/main/PRIVACY.md`.
9. Enviar. A revisão da Amazon costuma levar de alguns dias a 2 semanas.

## Testar a compra antes (Fire TV de verdade)

1. No Fire TV, instalar o **Amazon App Tester** (Appstore).
2. Copiar `amazon.sdktester.json` (nesta pasta) para `/sdcard/` do Fire TV:
   `adb push loja/amazon.sdktester.json /sdcard/`
3. `adb shell setprop debug.amazon.sandboxmode debug`
4. Instalar `dist/amazon/LibertyXFolder-FireTV.apk`, abrir a tela do Pro e
   comprar — a compra é de mentira, feita pelo App Tester.
5. Para voltar ao normal: `adb shell setprop debug.amazon.sandboxmode none`.

O emulador de TV daqui não tem Appstore: lá a tela do Pro mostra "Amazon
Appstore não respondeu" (conferido em 2026-10-19) — é o comportamento certo
num aparelho sem a loja.

---

## Textos da página

### English (US) — padrão

**Title:** LibertyX Folder

**Short description (Fire TV mostra esta):**
The files on your TV and on your computer, opened and organized with the
remote. Finds the computers and NAS on your home network by itself (SMB), opens
their videos and photos without downloading, and copies between TV and
computer. Free for the files on your device; the network is free for 7 days,
then a one-time Pro purchase.

**Long description:**

The files on your TV and on your computer, opened and organized with the remote.

LibertyX Folder is a file manager made for Fire TV. The gold highlight shows
where you are; the arrows move, OK opens and holding OK selects. The first time
you open it, an animated guide shows every tool in a minute. On Fire tablets it
works by touch, with everything unlocked.

★ YOUR HOME NETWORK, WITHOUT TYPING ADDRESSES
• When it opens, the app finds the computers, NAS and routers with a disk on your
  network by itself (Windows, macOS, Linux, Samba).
• Sign in with a user name and password, or as a guest on open folders.
• Videos, music and photos on the server open straight in your favorite player —
  read piece by piece, no download first. Fast-forward and rewind work.
• Copy and move between the TV and the computer, with progress on screen.

★ THE FILES ON YOUR TV AND USB DRIVE
• Ready-made categories: images, videos, audio, documents, downloads,
  installers (APK) and archives.
• List or grid, sorting by name, date, size or type, hidden files.
• A folder trail to go back several levels at once.
• Select several, move, copy, rename, delete, share and open with another app.
  Nothing is overwritten: a duplicate becomes "name (1)".
• Search across the whole device, and inside a folder with all its subfolders —
  even on a network server.

★ 7 DAYS WITH EVERYTHING UNLOCKED (FIRE TV)
The files on your TV are free forever. The network (SMB servers, automatic
discovery, opening and copying from the network) comes unlocked for the first
7 days; after that it becomes LibertyX Folder Pro: a one-time purchase, no
subscription, valid on every Fire TV with your Amazon account. On Fire tablets
everything is free.

★ NO ADS, NO TRACKING
Nothing leaves your device for the developer. Server passwords stay encrypted
on the device itself.

From the maker of LibertyX Player.

**Feature bullets:**
- Finds the computers and NAS on your home network by itself (SMB)
- Opens server videos and photos in your player without downloading first
- Copy and move between the TV, USB drives and your computer
- Made for the remote: gold focus, animated guide, long-press to select
- No ads, no tracking — passwords stay encrypted on the device

**Keywords:** file manager, file explorer, smb, samba, nas, network, lan, usb,
files, fire tv, remote, windows share

**IAP — LibertyX Folder Pro (descrição):** Unlocks the network for good: SMB
servers, automatic discovery, opening and copying from your computer or NAS.
One-time purchase, no subscription.

### Português (Brasil)

**Descrição curta:**
Os arquivos da sua TV e do seu computador, abertos e organizados pelo controle
remoto. Acha sozinho os computadores e NAS da sua rede (SMB), abre vídeos e
fotos deles sem baixar antes e copia entre TV e computador. Grátis para os
arquivos do aparelho; a rede é grátis por 7 dias, depois uma compra única Pro.

**Descrição longa:** a da Play (`FICHA.md`), trocando: "feito para Android TV e
Google TV" → "feito para Fire TV"; acrescentar "No tablet Fire funciona pelo
toque, com tudo liberado."; e "todas as TVs da sua conta Google" → "todos os
Fire TV da sua conta Amazon. No tablet Fire é tudo grátis."

**Destaques:**
- Acha sozinho os computadores e NAS da sua rede (SMB)
- Abre vídeos e fotos do servidor no seu player, sem baixar antes
- Copia e move entre a TV, o pendrive e o computador
- Feito para o controle remoto: foco dourado, guia animado, segurar OK seleciona
- Sem anúncios, sem rastreio — as senhas ficam cifradas no aparelho

**Palavras-chave:** gerenciador de arquivos, explorador, smb, samba, nas, rede,
pendrive, arquivos, fire tv, controle remoto, compartilhamento windows

### Español

**Descripción corta:**
Los archivos de tu TV y de tu computadora, abiertos y organizados con el
control remoto. Encuentra sola las computadoras y NAS de tu red (SMB), abre sus
videos y fotos sin descargarlos antes y copia entre la TV y la computadora.
Gratis para los archivos del dispositivo; la red es gratis 7 días, después una
compra única Pro.

**Descripción larga:** la de la Play (`FICHA.md`, versión en español),
cambiando "hecho para Android TV y Google TV" → "hecho para Fire TV", sumando
"En las tablets Fire funciona con el toque, con todo desbloqueado." y "todas
las TV de tu cuenta de Google" → "todos los Fire TV de tu cuenta de Amazon. En
las tablets Fire todo es gratis."

**Viñetas:**
- Encuentra sola las computadoras y NAS de tu red (SMB)
- Abre videos y fotos del servidor en tu reproductor, sin descargar antes
- Copia y mueve entre la TV, el pendrive y la computadora
- Hecho para el control remoto: foco dorado, guía animada, mantener OK selecciona
- Sin anuncios, sin rastreo: las contraseñas quedan cifradas en el dispositivo
