#!/usr/bin/env python3
"""
O ícone do LibertyX Folder, gerado por geometria.

O símbolo é o mesmo da família LibertyX — duas fitas de ouro cruzadas em X,
trançadas nos cruzamentos que ficam à vista — mas o miolo muda: onde o Player
tem o play, aqui tem uma pasta. Um vão de verdade é recortado em volta dela,
por subtração de formas (shapely) e não por traço da cor do fundo: assim o
mesmo desenho serve no ícone adaptativo, no monocromático do Android 13 e
dentro do app, sobre qualquer fundo.

Escreve em app/src/main/res e app/src/tv/res:
  drawable/ic_launcher_{foreground,background,monochrome}.xml, drawable/ic_marca.xml
  tv/drawable-{mdpi,xhdpi}/tv_banner.png     (a faixa da tela inicial da TV)
e Scripts/play-512.png, o ícone quadrado para a loja.

minSdk é 26, então não há mipmap PNG a gerar: todo aparelho aqui tem ícone
adaptativo.

Dependências: pip install shapely resvg-py pillow
"""
import io, math, os
import resvg_py
from PIL import Image, ImageDraw, ImageFont
from shapely import affinity
from shapely.geometry import Polygon
from shapely.ops import unary_union

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, "../app/src/main/res")
TV = os.path.join(AQUI, "../app/src/tv/res")
FONT = os.path.join(RES, "font")

OURO = [(0, "#FFF3CC"), (0.38, "#F5C84E"), (0.75, "#C98A1E"), (1, "#7A4A0E")]
OURO_P = [(0, "#FFF6D6"), (0.45, "#FBC501"), (1, "#B97A06")]   # o amarelo do app no miolo
FUNDO = [(0, "#2E2010"), (0.55, "#150E07"), (1, "#070403")]
C = (54, 54)


def elipse(rx, ry, ang, n=360):
    p = Polygon([(54 + rx * math.cos(t), 54 + ry * math.sin(t)) for t in (2 * math.pi * i / n for i in range(n))])
    return affinity.rotate(p, ang, origin=C)


def pasta(w=24.5, h=18.8, aba=0.44, alt_aba=3.5, inclina=2.9, r=1.8, cx=54, cy=54):
    """A pasta do miolo: corpo, aba erguida à esquerda e cantos arredondados.

    Os cantos saem de erodir e dilatar de volta (shapely), e não de curvas à
    mão: o mesmo raio vale no canto de fora e no degrau da aba, que é o que
    faz a forma parecer desenhada e não montada.
    """
    x0, y0 = cx - w / 2, cy - h / 2
    xa = x0 + w * aba
    p = Polygon([
        (x0, y0), (xa, y0), (xa + inclina, y0 + alt_aba), (x0 + w, y0 + alt_aba),
        (x0 + w, y0 + h), (x0, y0 + h),
    ])
    return p.buffer(-r, join_style="round").buffer(r, join_style="round")


def marca(rx=34, ry=11.5, irx=28.5, iry=10, ang=45, folga=2.4, trama=1.1, **kw):
    A = elipse(rx, ry, -ang).difference(elipse(irx, iry, -ang))   # sobe para a direita
    B = elipse(rx, ry, ang).difference(elipse(irx, iry, ang))
    miolo = pasta(**kw)
    # trama: nos cruzamentos que ficam à vista, uma fita passa por cima da outra
    if trama:
        cruz = A.intersection(B)
        pts = [g.centroid for g in cruz.geoms] if cruz.geom_type == "MultiPolygon" else [cruz.centroid]
        for i, p in enumerate(sorted(pts, key=lambda p: math.atan2(p.y - 54, p.x - 54))):
            zona = p.buffer(7)
            if i % 2 == 0: B = B.difference(A.intersection(zona).buffer(trama))
            else:          A = A.difference(B.intersection(zona).buffer(trama))
    fitas = unary_union([A, B]).difference(miolo.buffer(folga, join_style="round"))
    return fitas, miolo


def para_path(g, nd=2):
    polys = getattr(g, "geoms", [g])
    d = []
    for p in polys:
        for anel in [p.exterior, *p.interiors]:
            c = list(anel.simplify(0.01).coords)[:-1]
            d.append("M" + " ".join(f"{x:.{nd}f},{y:.{nd}f}" for x, y in c) + "Z")
    return "".join(d)


def stops(L): return "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in L)


def png(svg, px, h=None):
    b = resvg_py.svg_to_bytes(svg_string=svg, width=px, height=h or px)
    return Image.open(io.BytesIO(bytes(b))).convert("RGBA")


ESC = 0.9   # o símbolo dentro do círculo seguro de 66 de 108
fitas, miolo = marca()
fitas = affinity.scale(fitas, ESC, ESC, origin=C)
miolo = affinity.scale(miolo, ESC, ESC, origin=C)
PF, PM = para_path(fitas), para_path(miolo)
def e(v): return 54 + (v - 54) * ESC
G_OURO = (e(26), e(26), e(82), e(82)); G_P = (e(40), e(42), e(68), e(66))


def vd_grad(tipo, attrs, lst):
    itens = "\n".join(f'                    <item android:offset="{o}" android:color="#FF{c[1:]}" />' for o, c in lst)
    return ('            <aapt:attr name="android:fillColor">\n'
            f'                <gradient android:type="{tipo}" {attrs}>\n{itens}\n                </gradient>\n'
            '            </aapt:attr>')


CAB = '<?xml version="1.0" encoding="utf-8"?>\n'


def vector(corpo, w=108, vw=108, vh=108, h=None, tx=0, ty=0, comentario=""):
    h = h or w
    g0, g1 = (f'    <group android:translateX="{tx}" android:translateY="{ty}">\n', '    </group>\n') if (tx or ty) else ("", "")
    return (CAB + comentario +
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:aapt="http://schemas.android.com/aapt"\n'
        f'    android:width="{w}dp" android:height="{h}dp"\n    android:viewportWidth="{vw}" android:viewportHeight="{vh}">\n'
        + g0 + corpo + g1 + '</vector>\n')


def corpo_marca(mono=False):
    if mono:
        return (f'    <path android:fillType="evenOdd" android:fillColor="#FFFFFFFF"\n        android:pathData="{PF}" />\n'
                f'    <path android:fillColor="#FFFFFFFF"\n        android:pathData="{PM}" />\n')
    x1, y1, x2, y2 = G_OURO; a1, b1, a2, b2 = G_P
    return (f'    <path android:fillType="evenOdd"\n        android:pathData="{PF}">\n'
            + vd_grad("linear", f'android:startX="{x1:.2f}" android:startY="{y1:.2f}" android:endX="{x2:.2f}" android:endY="{y2:.2f}"', OURO) + '\n    </path>\n'
            f'    <path\n        android:pathData="{PM}">\n'
            + vd_grad("linear", f'android:startX="{a1:.2f}" android:startY="{b1:.2f}" android:endX="{a2:.2f}" android:endY="{b2:.2f}"', OURO_P) + '\n    </path>\n')


COM_F = ('<!-- O símbolo do LibertyX Folder: as duas fitas de ouro cruzadas em X da\n'
         '     família, trançadas nos cruzamentos à vista, e a pasta recortada no meio.\n'
         '     Gerado por geometria (Scripts/icone.py), não desenhado à mão: o vão em\n'
         '     volta da pasta é um buraco de verdade, e não um traço da cor do fundo,\n'
         '     então o mesmo desenho serve sobre qualquer fundo e no monocromático. -->\n')
open(f"{RES}/drawable/ic_launcher_foreground.xml", "w").write(vector(corpo_marca(), comentario=COM_F))
open(f"{RES}/drawable/ic_launcher_monochrome.xml", "w").write(vector(corpo_marca(True),
    comentario='<!-- O mesmo símbolo numa cor só, para o ícone temático do Android 13+. -->\n'))
x0, y0, x1, y1 = fitas.union(miolo).bounds; lado = max(x1 - x0, y1 - y0) + 1
open(f"{RES}/drawable/ic_marca.xml", "w").write(vector(corpo_marca(), w=48, vw=round(lado, 2), vh=round(lado, 2),
    tx=round(-(54 - lado / 2), 2), ty=round(-(54 - lado / 2), 2),
    comentario='<!-- O símbolo sem a margem do ícone adaptativo, para a marca dentro do app. -->\n'))
fundo_xml = ('    <path android:pathData="M0,0h108v108h-108z">\n'
             + vd_grad("radial", 'android:centerX="54" android:centerY="34" android:gradientRadius="82"', FUNDO) + '\n    </path>\n')
open(f"{RES}/drawable/ic_launcher_background.xml", "w").write(vector(fundo_xml,
    comentario='<!-- Preto quente com uma luz vinda de cima: o ouro parece iluminado, não\n     colado sobre um preto chapado. -->\n'))


def svg(size, fundo=True):
    x1, y1, x2, y2 = G_OURO; a1, b1, a2, b2 = G_P
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="{size}" height="{size}">
<defs><radialGradient id="f" cx="54" cy="34" r="82" gradientUnits="userSpaceOnUse">{stops(FUNDO)}</radialGradient>
<linearGradient id="o" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" gradientUnits="userSpaceOnUse">{stops(OURO)}</linearGradient>
<linearGradient id="p" x1="{a1}" y1="{b1}" x2="{a2}" y2="{b2}" gradientUnits="userSpaceOnUse">{stops(OURO_P)}</linearGradient></defs>
{'<rect width="108" height="108" fill="url(#f)"/>' if fundo else ''}
<path fill-rule="evenodd" fill="url(#o)" d="{PF}"/><path fill="url(#p)" d="{PM}"/></svg>'''


# Loja: 512 quadrado cheio, já com o corte de 18 que o adaptativo esconde.
g = png(svg(1), 512 * 108 // 72); c = int(g.size[0] * 18 / 108)
g.crop((c, c, g.size[0] - c, g.size[0] - c)).resize((512, 512), Image.LANCZOS).convert("RGB").save(os.path.join(AQUI, "play-512.png"))


def render(s, w, h):
    return Image.open(io.BytesIO(bytes(resvg_py.svg_to_bytes(svg_string=s, width=w, height=h)))).convert("RGBA")


def banner(W, H, slogan=None):
    """A faixa da tela inicial da TV: símbolo + nome, o conjunto centrado."""
    S = 2; w, h = W * S, H * S
    vh = 320 * H / W
    fundo = render(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 {vh:.2f}" width="{w}" height="{h}"><defs>'
                   f'<radialGradient id="f" cx="110" cy="40" r="260" gradientUnits="userSpaceOnUse">{stops(FUNDO)}</radialGradient></defs>'
                   f'<rect width="320" height="{vh:.2f}" fill="url(#f)"/></svg>', w, h)
    sobe = int(h * 0.07) if slogan else 0
    m = render(svg(108, fundo=False), h * 2, h * 2)
    m = m.crop(m.getbbox())
    alto = int(h * 0.56); m = m.resize((int(m.width * alto / m.height), alto), Image.LANCZOS)
    d = ImageDraw.Draw(fundo)
    f1 = ImageFont.truetype(os.path.join(FONT, "manrope_extrabold.ttf"), int(h * 0.2))
    f2 = ImageFont.truetype(os.path.join(FONT, "manrope_semibold.ttf"), int(h * 0.062))
    larg_nome = d.textlength("LibertyX", font=f1)
    vao = int(h * 0.09)
    x = int((w - (m.width + vao + larg_nome)) // 2)
    fundo.alpha_composite(m, (x, (h - alto) // 2 - sobe))
    x += m.width + vao; base = int(h * 0.54) - sobe
    d.text((x, base), "Liberty", font=f1, fill=(240, 240, 240), anchor="ls")
    x2 = x + d.textlength("Liberty", font=f1)
    d.text((x2, base), "X", font=f1, fill=(0xFB, 0xC5, 0x01), anchor="ls")
    # FOLDER espaçado na largura exata do nome
    letras = "FOLDER"; larg = sum(d.textlength(c, font=f2) for c in letras)
    esp = (larg_nome - larg - 4) / (len(letras) - 1); cx = x + 3
    for ch in letras:
        d.text((cx, base + int(h * 0.13)), ch, font=f2, fill=(235, 235, 245, 150), anchor="ls")
        cx += d.textlength(ch, font=f2) + esp
    if slogan:
        f3 = ImageFont.truetype(os.path.join(FONT, "manrope_semibold.ttf"), int(h * 0.07))
        d.text((w // 2, int(h * 0.9)), slogan, font=f3, fill=(0xFB, 0xC5, 0x01, 230), anchor="ms")
    return fundo.resize((W, H), Image.LANCZOS).convert("RGB")


for pasta_dens, (W, H) in {"drawable-mdpi": (320, 180), "drawable-xhdpi": (640, 360)}.items():
    os.makedirs(os.path.join(TV, pasta_dens), exist_ok=True)
    banner(W, H).save(os.path.join(TV, pasta_dens, "tv_banner.png"), optimize=True)
print("ok")
