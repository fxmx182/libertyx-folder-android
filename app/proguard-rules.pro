# Regras do R8 para o release e a loja.
#
# O smbj e o dcerpc montam pacotes SMB/RPC com reflexão e um barramento de
# eventos (mbassador) que acha os ouvintes por anotação — encolher as classes
# deles quebra a conexão só em tempo de execução. Ficam inteiros. A
# BouncyCastle não: o smbj usa só a API leve dela por referência direta
# (MD4, HMAC, RC4, DES), que o R8 rastreia; o resto — incluindo os megabytes
# de criptografia pós-quântica que vêm junto — sai.
-keep class com.hierynomus.** { *; }
-keep class com.rapid7.** { *; }
-keep class net.engio.mbassy.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
-dontwarn com.sun.**
-dontwarn javax.naming.**
-dontwarn java.lang.management.**

# java.rmi não existe no Android: o dcerpc só o cita em caminhos de erro, e já
# era assim sem o R8.
-dontwarn java.rmi.**

# Cobrança da Amazon (só na variante tvAmazon): a própria Amazon pede para não
# encolher o SDK — ele acha as próprias classes por nome em tempo de execução.
-dontwarn com.amazon.**
-keep class com.amazon.** { *; }
-keepattributes *Annotation*
