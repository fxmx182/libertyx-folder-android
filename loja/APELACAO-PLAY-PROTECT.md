# Apelação ao Google Play Protect — com.mauricio.libertyx.folder

Formulário (feito pelo dono da conta, logado com o Gmail da conta de
desenvolvedor): https://support.google.com/googleplay/android-developer/contact/protectappeals

## Dados

- Nome do pacote: `com.mauricio.libertyx.folder`
- Conta de desenvolvedor: Germano Mauricio — ID 6275966982932833578
- Aviso recebido: "App nocivo bloqueado — Este app pode instalar outros apps
  potencialmente nocivos sem a sua permissão" (TV e celular, 04/10/2026)
- Certificados (SHA-256) usados fora da Play, ambos registrados e verificados
  na Verificação de desenvolvedor Android:
  - `AB:56:E2:75:56:0B:0C:98:AE:D9:DB:5A:E7:F9:A3:7D:A3:95:4F:DC:7A:05:57:3A:8F:CA:A9:11:2C:B6:89:AF`
  - `B4:3A:C4:C1:4C:FF:44:C5:AE:0D:CD:E0:48:C7:1B:B7:CB:BD:12:2F:D3:40:BC:BF:87:EE:B0:AF:C5:2F:CB:87`
- APK para anexar, se pedirem: compilar a versão antiga ou usar o .aab enviado
  ao teste interno (versão 3974812).

## Texto (inglês, que é o que o formulário pede)

LibertyX Folder (com.mauricio.libertyx.folder) is a file manager for Android TV
and phones, developed by me and published through my Play Console account
(ID 6275966982932833578). Since October 4, 2026, Play Protect blocks every
version of it as "can install potentially harmful apps without your
permission".

The app never installs apps by itself. It does not use the PackageInstaller
API or ACTION_INSTALL_PACKAGE; it only declared REQUEST_INSTALL_PACKAGES so
that tapping an .apk file opened the system installer, which always asks the
user to confirm — the standard behavior of a file manager. It does not
download or execute code. Its network access is limited to SMB file shares
on the user's local network, chosen by the user.

To remove any doubt, the current version no longer requests
REQUEST_INSTALL_PACKAGES at all. Both signing certificates I use outside
Google Play are registered and verified in Android developer verification.

I am asking you to review the classification of com.mauricio.libertyx.folder.

## Enquanto a apelação não sai

O app passou a ser `com.mauricio.libertyx.files` (sem a permissão de instalar
apps, chave registrada). Se a apelação for aceita, dá para voltar ao nome
antigo — mas não é preciso: o novo já resolve.
