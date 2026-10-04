# Lançar o LibertyX Folder para TV na Play Store — passo a passo

O que já está pronto no código está marcado ✅. O resto é feito no navegador, no
[Play Console](https://play.google.com/console), pela conta do dono — a mesma
conta de desenvolvedor do LibertyX Player serve (não paga de novo).

## Estado no Play Console (2026-10-04)

- **App em uso: LibertyX Folder com o pacote `com.mauricio.libertyx.files`**
  (ID 4973238286129143138). O primeiro app criado, com o pacote
  `com.mauricio.libertyx.folder` (ID 4972154570058753375), foi abandonado: o
  Google Play Protect marcou aquele pacote (ver `APELACAO-PLAY-PROTECT.md`).
- Feito no app novo: formato Android TV; teste interno publicado (versão
  3979542, lista de testadores "Eu"); produto `libertyx_folder_pro` ativo a
  US$ 4,99; página da loja em português com textos, ícone, destaque, banner de TV
  e capturas (TV, e as mesmas nos campos obrigatórios de celular e tablet).
- Falta: traduções da página (en/es), endereço público da política de
  privacidade, questionários de "Conteúdo do app" e o teste fechado.
- **No Console em português, preço com vírgula:** "4.99" vira 499.

## O modelo

- **Só a TV** vai para a loja agora. O app de celular fica de fora (o
  `bundleLoja` nem gera a variante de celular).
- **App grátis + compra única "LibertyX Folder Pro"** (produto
  `libertyx_folder_pro`, US$ 4,99).
- **7 dias com tudo liberado**, contados da primeira abertura. Depois, os
  arquivos da TV continuam grátis para sempre e a **rede** (servidores SMB,
  busca automática, abrir e copiar da rede) pede o Pro.
- **A TV não tem pasta segura.** Quem já tinha uma (só os APKs de antes,
  instalados à mão) acha em Configurações como retirar os arquivos e apagá-la.

## O que o código já faz

- ✅ Compra pela Google Play Billing 8 (cópia da do Player): reconhecimento
  automático (sem ele a Play estorna em 3 dias), pagamento pendente (boleto/Pix)
  e "Restaurar compra".
- ✅ Teste de 7 dias contado pelo app; a data vai no backup do Android (só ela),
  para reinstalar não recomeçar.
- ✅ Tela do Pro, selo "PRO GRÁTIS · N DIAS" no início, rede travada levando à
  tela do Pro, e o guia de uso de 10 páginas na primeira abertura (revisto em
  Configurações › Guia de uso).
- ✅ `./gradlew bundleLoja` gera `dist/play/LibertyXFolder-TV.aab`, assinado
  com a **chave de envio** (a mesma do Player, em `/DATA/Claudinho/chaves/`).
  Nesse pacote o Pro nunca vem liberado.
- ✅ Número de versão da TV termina em **2** (quando o celular entrar na mesma
  página, terminará em 1 — a Play recusa números repetidos).
- ✅ Política de privacidade: [`../PRIVACY.md`](../PRIVACY.md). **Falta um
  endereço público** para ela (ver passo 7).
- ✅ **Idiomas**: o app segue o idioma do aparelho — inglês (padrão para
  qualquer idioma sem tradução), português e espanhol; no Android 13+ dá para
  escolher um só para o app em Configurações › Apps › Idioma. O pacote leva os
  três (sem divisão por idioma), para essa escolha funcionar.
- ✅ Textos da página nos três idiomas: [`FICHA.md`](FICHA.md). Imagens em
  `imagens/`, uma série por idioma.

## Passo a passo no Play Console

1. **Criar app**: nome *LibertyX Folder*, idioma padrão *Português (Brasil)*,
   *App*, *Gratuito* (quem cobra é a compra dentro do app).
2. **Formatos** (*Configurações › Configuração avançada › Formatos*): adicione
   **Android TV**. Se o Console exigir uma versão também na faixa principal,
   suba nela o mesmo `.aab` de TV: como ele exige `leanback`, a Play só o
   oferece para TVs.
3. **Primeiro envio** em *Teste › Teste interno*: suba
   `dist/play/LibertyXFolder-TV.aab` e aceite o **Play App Signing**. O produto
   só pode ser criado depois de um envio com a biblioteca de cobrança.
4. **Produto** em *Monetizar › Produtos › Produtos no app*: os dados estão na
   `FICHA.md`. O ID tem de ser **exatamente** `libertyx_folder_pro`. Preço
   US$ 4,99 → "Atualizar taxas de câmbio"; ajuste à mão os países que quiser
   redondos. Ativar.
5. **Testadores de licença** (*Configurações › Teste de licença*): o seu Gmail.
   Compras dessa conta não cobram — é como testar a compra na sua TV.
6. **Países e traduções da página**: o app fala inglês, português e espanhol
   (e cai no inglês em qualquer outro idioma), então dá para distribuir em
   todos os países. Em *Página da loja › Gerenciar traduções*, acrescente
   **inglês (en-US)** e **espanhol (es-419 e es-ES)** com os textos da `FICHA.md`
   e as imagens `destaque-1024x500-en/es.png` e `tv-en-*` / `tv-es-*.jpg`.
7. **Conteúdo do app** (*Política › Conteúdo do app*):
   - *Política de privacidade*: precisa de um endereço público com o texto de
     `PRIVACY.md` (já em inglês, português e espanhol). Opções: a pasta do LibertyX Player no GitHub (que já é
     público), ou uma página no seu domínio.
   - *Acesso ao app*: "Todas as funcionalidades estão disponíveis sem
     restrições" — o revisor usa os 7 dias de teste.
   - *Anúncios*: Não. *Público-alvo*: 18+. *Classificação*: questionário, tudo
     "não".
   - *Segurança dos dados*: **não coleta nem compartilha dados**. Senhas de
     servidor ficam só no aparelho; a compra é processada pelo Google.
   - **Declarações de permissão** — o Console pede uma por uma:
     - *Acesso a todos os arquivos* (`MANAGE_EXTERNAL_STORAGE`): uso principal
       **gerenciador de arquivos**.
     - *Fotos e vídeos* (`READ_MEDIA_IMAGES/VIDEO`): gerenciador de arquivos;
       usado quando a TV não tem a tela de acesso a todos os arquivos.
     - *Serviço em primeiro plano* (`dataSync`): "copiar e mover arquivos entre
       a TV e computadores da rede, iniciado pelo usuário, com progresso".
       Pede um vídeo curto: grave a TV copiando uma pasta e suba no YouTube
       como "não listado".
8. **Teste fechado** (*Teste › Teste fechado*): conta pessoal criada depois de
   nov/2023 exige, **por app**, **12 testadores por 14 dias seguidos** antes da
   produção — o do Player não vale para este. Eles precisam de uma TV com
   Google TV / Android TV.
9. **Página da loja**: textos da `FICHA.md`; ícone `imagens/icone-512.png`;
   destaque `imagens/destaque-1024x500-pt.png`; banner de TV
   `imagens/tv-banner-1280x720.png`; capturas de TV `imagens/tv-pt-*.jpg` (e as
   versões `-en`/`-es` em cada tradução, passo 6).
   Categoria *Ferramentas*. E-mail de contato: obrigatório e público.
10. **Revisão de TV**: a Play revisa à parte a qualidade em TV (tudo pelo
    controle remoto, banner, sem depender de toque). Já conferido no emulador.
11. **Produção**: cumprido o teste fechado, promova a versão. A primeira revisão
    costuma levar de 1 a 7 dias.

## Toda atualização depois disso

```bash
./gradlew bundleLoja
```

Suba `dist/play/LibertyXFolder-TV.aab` na faixa de Android TV.

## APKs da pasta `dist/`

- Compilados nesta máquina: Pro **liberado** — são os seus.
- Assinatura diferente da Play: o app da loja não instala por cima de um APK da
  pasta. Para trocar, desinstale antes.
- **Fire TV** não tem Google Play: lá não há como comprar. Quem tem Fire TV usa
  os 7 dias e depois só os arquivos da TV — a não ser que um dia o app vá
  também para a Amazon Appstore (não feito).
