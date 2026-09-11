# -*- coding: utf-8 -*-
"""Desenha a figura da questao 12 - solucao de arquitetura para publicacao de eventos.

    python scripts/gerar-diagrama.py

Gera docs/evidences/12-arquitetura-eventos.png.

O desenho e codigo, e nao um PNG solto arrastado para a pasta, por dois motivos: ele acompanha o
repositorio como qualquer outro artefato revisavel, e refazer a figura depois de uma correcao e
uma linha de comando em vez de meia hora de ferramenta grafica.

Decisoes de forma:
  - tons claros com contorno escuro, legivel em escala de cinza (o PDF pode ser impresso);
  - tracejado = o que existe no desenho mas nao no codigo (outbox e Event Store);
  - o fan-out do topico e o unico cruzamento de setas da figura, de proposito: e o ponto que a
    questao 11 pede para enxergar.
"""
import os

from PIL import Image, ImageDraw, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SAIDA = os.path.join(RAIZ, 'docs', 'evidences', '12-arquitetura-eventos.png')

L, A = 2240, 1290
FUNDO = (255, 255, 255)

TINTA = (24, 24, 32)
TRACO = (70, 70, 90)
ROXO = (109, 40, 217)
AZUL = (14, 116, 190)
VERDE = (22, 120, 70)
CINZA = (120, 120, 135)

DOMINIO = (243, 240, 255)
APLICACAO = (235, 245, 255)
BROKER = (255, 246, 232)
FILA = (240, 240, 245)
CONSUMIDOR = (236, 250, 241)
OPCIONAL = (250, 250, 252)

F = 'C:/Windows/Fonts/'
titulo = ImageFont.truetype(F + 'segoeuib.ttf', 40)
rotulo = ImageFont.truetype(F + 'segoeuib.ttf', 28)
corpo = ImageFont.truetype(F + 'segoeui.ttf', 24)
miudo = ImageFont.truetype(F + 'segoeui.ttf', 21)
mono = ImageFont.truetype(F + 'consola.ttf', 21)
monob = ImageFont.truetype(F + 'consolab.ttf', 24)

img = Image.new('RGB', (L, A), FUNDO)
d = ImageDraw.Draw(img)


def centrado(texto, fonte, x, y, cor=TINTA):
    larg = d.textlength(texto, font=fonte)
    d.text((x - larg / 2, y), texto, font=fonte, fill=cor)


def caixa(x, y, larg, alt, titulo_txt, linhas=(), fill=DOMINIO, borda=TRACO,
          fonte_titulo=rotulo, tracejada=False, raio=16):
    """Retangulo arredondado com titulo em negrito e linhas menores embaixo."""
    caixa_xy = (x, y, x + larg, y + alt)
    if tracejada:
        d.rounded_rectangle(caixa_xy, radius=raio, fill=fill)
        _contorno_tracejado(caixa_xy, raio, borda)
    else:
        d.rounded_rectangle(caixa_xy, radius=raio, fill=fill, outline=borda, width=3)

    meio = x + larg / 2
    altura_texto = 34 + len(linhas) * 28
    topo = y + (alt - altura_texto) / 2
    centrado(titulo_txt, fonte_titulo, meio, topo)
    for i, linha in enumerate(linhas):
        fonte = mono if linha.startswith('`') else miudo
        centrado(linha.strip('`'), fonte, meio, topo + 38 + i * 28, TRACO)


def _contorno_tracejado(xy, raio, cor, passo=14):
    x0, y0, x1, y1 = xy
    for x in range(int(x0 + raio), int(x1 - raio), passo * 2):
        d.line([(x, y0), (min(x + passo, x1 - raio), y0)], fill=cor, width=3)
        d.line([(x, y1), (min(x + passo, x1 - raio), y1)], fill=cor, width=3)
    for y in range(int(y0 + raio), int(y1 - raio), passo * 2):
        d.line([(x0, y), (x0, min(y + passo, y1 - raio))], fill=cor, width=3)
        d.line([(x1, y), (x1, min(y + passo, y1 - raio))], fill=cor, width=3)


def seta(p0, p1, cor=TRACO, largura=3, cabeca=15, tracejada=False):
    x0, y0 = p0
    x1, y1 = p1
    if tracejada:
        _linha_tracejada(p0, p1, cor, largura)
    else:
        d.line([p0, p1], fill=cor, width=largura)

    dx, dy = x1 - x0, y1 - y0
    comp = max((dx ** 2 + dy ** 2) ** 0.5, 1)
    ux, uy = dx / comp, dy / comp
    px, py = -uy, ux
    d.polygon([(x1, y1),
               (x1 - ux * cabeca - px * cabeca * 0.55, y1 - uy * cabeca - py * cabeca * 0.55),
               (x1 - ux * cabeca + px * cabeca * 0.55, y1 - uy * cabeca + py * cabeca * 0.55)],
              fill=cor)


def _linha_tracejada(p0, p1, cor, largura, passo=13):
    x0, y0 = p0
    x1, y1 = p1
    comp = max(((x1 - x0) ** 2 + (y1 - y0) ** 2) ** 0.5, 1)
    passos = int(comp // passo)
    for i in range(0, passos, 2):
        a = i / passos
        b = min((i + 1) / passos, 1)
        d.line([(x0 + (x1 - x0) * a, y0 + (y1 - y0) * a),
                (x0 + (x1 - x0) * b, y0 + (y1 - y0) * b)], fill=cor, width=largura)


def etiqueta(texto, x, y, fonte=miudo, cor=TRACO, fundo=FUNDO):
    larg = d.textlength(texto, font=fonte)
    d.rectangle((x - larg / 2 - 8, y - 4, x + larg / 2 + 8, y + 28), fill=fundo)
    d.text((x - larg / 2, y), texto, font=fonte, fill=cor)


# ---------------------------------------------------------------- cabecalho
d.text((60, 48), 'Publicação de eventos de domínio · contexto de Pagamento', font=titulo, fill=TINTA)
d.text((60, 100), 'E-commerce · DR4-TP2 · o agregado registra o fato; a aplicação publica depois do commit',
       font=corpo, fill=TRACO)
d.line([(60, 148), (L - 60, 148)], fill=(220, 220, 230), width=3)

# ---------------------------------------------------------------- coluna 1: o contexto
d.rounded_rectangle((60, 190, 700, 1130), radius=22, outline=(210, 205, 235), width=3)
d.text((84, 208), 'CONTEXTO DE PAGAMENTO', font=miudo, fill=ROXO)

caixa(100, 262, 560, 190, 'Pagamento · Aggregate Root',
      ['1 · exigirPendente()   invariante',
       '2 · status = APROVADO  estado',
       '3 · registrar(evento)  fato'], fill=DOMINIO, borda=ROXO)
etiqueta('acumula, não publica', 380, 458, miudo, ROXO)

seta((380, 496), (380, 566), ROXO)

caixa(100, 570, 560, 175, 'PagamentoAppService',
      ['@Transactional',
       'salvar → drenar → publicar → limpar'], fill=APLICACAO, borda=AZUL)

seta((380, 745), (380, 815), AZUL)

caixa(100, 820, 560, 150, 'PublicadorDeEventos  (porta)',
      ['PublicadorDeEventosSpring',
       'trocar por Kafka muda só esta classe'], fill=APLICACAO, borda=AZUL)

caixa(100, 1000, 560, 105, 'Outbox · mesma transação',
      ['garantia de entrega, se necessário'], fill=OPCIONAL, borda=CINZA, tracejada=True)
seta((380, 970), (380, 998), CINZA, tracejada=True)

# ---------------------------------------------------------------- coluna 2: o topico
etiqueta('após o commit', 748, 752, miudo, AZUL)
seta((660, 880), (832, 792), AZUL)

AMBAR = (200, 130, 30)
d.rounded_rectangle((835, 545, 1165, 875), radius=20, fill=BROKER, outline=AMBAR, width=3)
centrado('TÓPICO', rotulo, 1000, 582, AMBAR)
centrado('pagamentos', monob, 1000, 632, TINTA)
centrado('uma cópia do fato', corpo, 1000, 692, TRACO)
centrado('para cada assinante', corpo, 1000, 722, TRACO)
centrado('publish / subscribe', miudo, 1000, 790, AMBAR)

seta((1000, 875), (1000, 960), CINZA, tracejada=True)
caixa(835, 965, 330, 140, 'Event Store', ['append-only · imutável',
                                          'o histórico do que aconteceu'],
      fill=OPCIONAL, borda=CINZA, tracejada=True)

# ---------------------------------------------------------------- coluna 3: filas e consumidores
filas = [
    ('fila  pedidos.pagamento', 'Contexto Pedido', 'marca o pedido como pago', 230),
    ('fila  notificacoes.pagamento', 'Notificações', 'avisa o cliente', 545),
    ('fila  fiscal.pagamento', 'Fiscal', 'emite a nota', 860),
]

for nome_fila, consumidor, acao, y in filas:
    seta((1165, 710), (1290, y + 62), (200, 130, 30))
    caixa(1295, y, 380, 125, nome_fila, ['cada mensagem para', 'um único consumidor'],
          fill=FILA, borda=TRACO, fonte_titulo=corpo)
    seta((1675, y + 62), (1770, y + 62), TRACO)
    caixa(1775, y, 400, 125, consumidor, [acao], fill=CONSUMIDOR, borda=VERDE)

d.text((1295, 1010), 'A fila é onde a concorrência acontece: duas instâncias de Notificações',
       font=miudo, fill=TRACO)
d.text((1295, 1038), 'lendo a mesma fila dividem as mensagens, e nenhuma chega duas vezes.',
       font=miudo, fill=TRACO)
d.text((1295, 1080), 'O tópico distribui; a fila reparte. É por isso que os dois aparecem juntos:',
       font=miudo, fill=TRACO)
d.text((1295, 1108), 'um tópico e, atrás dele, uma fila por consumidor interessado.',
       font=miudo, fill=TRACO)

# ---------------------------------------------------------------- legenda
d.line([(60, 1178), (L - 60, 1178)], fill=(220, 220, 230), width=3)
d.text((60, 1200), 'Traço contínuo: implementado no repositório.', font=miudo, fill=TRACO)
d.text((640, 1200), 'Tracejado: previsto no desenho, fora do escopo do código (outbox e Event Store).',
       font=miudo, fill=CINZA)

img.save(SAIDA)
print('gerado:', SAIDA, img.size)
