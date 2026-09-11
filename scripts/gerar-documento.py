# -*- coding: utf-8 -*-
"""Gera o documento de entrega do DR4-TP2 sobre o molde institucional do Infnet.

    python scripts/gerar-documento.py

Reaproveita do molde (docs/molde/andre_becker_DR4_TP1.docx) a capa, os estilos, o tema, o
cabecalho, os rodapes, a numeracao e as faixas de secao, e substitui apenas o corpo.

O texto vem de docs/RELATORIO_TP2.md, que e a unica fonte: o .docx e uma renderizacao dele, nao
uma segunda copia do conteudo. Corrigir uma resposta e editar o markdown e rodar este script.

Convencoes reconhecidas no markdown:
  ## N. enunciado    questao: vira o titulo "Questao N" mais a caixa com o enunciado copiado
  ## Titulo          secao final (Evidencias de codigo)
  > texto            caixa destacada (a nota de escopo)
  - item             lista com marcador
  1. item            lista numerada
  | a | b |          tabela
  ```                bloco de codigo monoespacado
  ![alt](caminho)    figura, com legenda numerada

O enunciado do TP2 dispensa sumario, entao nenhum indice e gerado.

Se o molde estiver travado (OneDrive sincronizando, ou aberto no Word), passe como primeiro
argumento um diretorio com o pacote ja desempacotado.
"""
import io
import os
import re
import shutil
import struct
import sys
import tempfile
import zipfile

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOLDE = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    RAIZ, 'docs', 'molde', 'andre_becker_DR4_TP1.docx')
# Fonte unica: o relatorio e o trabalho, e o .docx e a renderizacao dele para entrega. Chegamos a
# manter um resumo separado, e a conclusao foi que dois textos cobrindo as mesmas 14 questoes so
# criam divergencia na primeira correcao.
FONTE = os.path.join(RAIZ, 'docs', 'RELATORIO_TP2.md')
SAIDA = os.path.join(RAIZ, 'andre_becker_DR4_TP2.docx')
# Fora do projeto de proposito: diretorio temporario dentro do OneDrive fica travado pela
# sincronizacao no meio do build.
TRAB = os.path.join(tempfile.gettempdir(), '_pacote_docx_dr4_tp2')

DATA_ENTREGA = '10/09/2026'
MES_CAPA = 'Setembro/2026'

# Repositorio do TP2. Enquanto ele nao existir, o documento sai apontando para este endereco;
# depois do push, confira se o nome bate.
REPO = 'https://github.com/andrebecker84/ecommerce-ddd-domain-events-TP2'
REPO_RELATORIO = REPO + '/blob/main/docs/RELATORIO_TP2.md'

INTRO = (
    'Este trabalho prático (DR4-TP2) foi desenvolvido para a disciplina Domain-Driven Design '
    '(DDD) e Arquitetura de Softwares Escaláveis com Java, do Bloco Engenharia de Softwares '
    'Escaláveis do Instituto Infnet. A entrega foi realizada individualmente e responde às '
    'quatorze questões do enunciado sobre Agregados e eventos de domínio. Conforme o enunciado, '
    'cada questão está numerada e acompanhada do seu enunciado original. Onde o enunciado '
    'original cita o projeto Pet Friends, este trabalho usa o projeto de e-commerce '
    'desenvolvido no DR4-TP1, do qual foi extraído o contexto delimitado de Pagamento. O '
    'código-fonte citado nas respostas, os testes automatizados e este mesmo relatório estão '
    'publicados no repositório indicado na seção de Compartilhamento.')

COMPARTILHAMENTO = (
    'O projeto pode ser acessado publicamente no repositório *GitHub* a seguir. O código-fonte '
    'está disponível com permissão de leitura e clonagem, incluindo o agregado Pagamento com o '
    'registro dos eventos, a abstração dos eventos de domínio, os eventos concretos, a porta de '
    'publicação com o seu adaptador e os vinte e oito testes automatizados. Nenhuma credencial, '
    'senha ou arquivo de ambiente foi versionado: a configuração sensível é fornecida por '
    'variáveis de ambiente e o repositório publica apenas o modelo .env.example, sem valores '
    'reais.')

REFERENCIAS = [
    'EVANS, Eric. Domain-Driven Design: Tackling Complexity in the Heart of Software. Boston: '
    'Addison-Wesley, 2003.',
    'FOWLER, Martin. Event Sourcing. martinfowler.com, 2005. Disponível em: '
    'https://martinfowler.com/eaaDev/EventSourcing.html.',
    'HOHPE, Gregor; WOOLF, Bobby. Enterprise Integration Patterns: Designing, Building, and '
    'Deploying Messaging Solutions. Boston: Addison-Wesley, 2003.',
    'RICHARDSON, Chris. Microservices Patterns: With Examples in Java. Shelter Island: Manning, '
    '2018.',
    'VERNON, Vaughn. Implementing Domain-Driven Design. Boston: Addison-Wesley, 2013.',
    'VERNON, Vaughn. Effective Aggregate Design. dddcommunity.org, 2011. Disponível em: '
    'https://www.dddcommunity.org/library/vernon_2011/.',
]

# ---------------------------------------------------------------- desempacota
if os.path.isdir(TRAB):
    shutil.rmtree(TRAB)
if os.path.isdir(MOLDE):
    shutil.copytree(MOLDE, TRAB)
else:
    with zipfile.ZipFile(MOLDE) as z:
        z.extractall(TRAB)

doc_path = os.path.join(TRAB, 'word', 'document.xml')
d = io.open(doc_path, encoding='utf-8').read()

# ------------------------------------------- formatacao medida no proprio molde
FONT_BODY = ('<w:rFonts w:asciiTheme="majorHAnsi" w:hAnsiTheme="majorHAnsi" '
             'w:cstheme="majorHAnsi"/>')
FONT_CODE = '<w:rFonts w:ascii="Consolas" w:hAnsi="Consolas" w:cs="Consolas"/>'
LARG = 9247  # area util da pagina, em twips


def esc(t):
    return t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


def runs(texto, base=FONT_BODY):
    """**negrito**, *italico*, `codigo` e [texto](url) viram runs do WordprocessingML."""
    saida = []
    # O link vem primeiro: dentro dele nao ha marcacao a interpretar, e o "!" na frente
    # significa imagem, tratada em outro lugar.
    for parte in re.split(r'((?<!!)\[[^\]]*\]\([^)]+\))', texto):
        m = re.match(r'^\[([^\]]*)\]\(([^)]+)\)$', parte)
        if m:
            saida.append(hyperlink(m.group(1), m.group(2), base))
        elif parte:
            saida.append(_runs_simples(parte, base))
    return ''.join(saida)


def hyperlink(texto, url, base):
    """Link externo: cada URL vira um Relationship novo no pacote."""
    _prox[0] += 1
    rid = 'rId%d' % _prox[0]
    links.append((rid, url))
    return ('<w:hyperlink r:id="%s" w:history="1"><w:r>'
            '<w:rPr><w:rStyle w:val="Hyperlink"/>%s</w:rPr>'
            '<w:t xml:space="preserve">%s</w:t></w:r></w:hyperlink>'
            % (rid, base, esc(texto)))


def _runs_simples(texto, base):
    texto = re.sub(r'!\[([^\]]*)\]\([^)]*\)', r'\1', texto)
    saida = []
    padrao = re.compile(r'\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`', re.S)
    pos = 0
    for m in padrao.finditer(texto):
        if m.start() > pos:
            saida.append((texto[pos:m.start()], None))
        if m.group(1) is not None:
            saida.append((m.group(1), 'b'))
        elif m.group(2) is not None:
            saida.append((m.group(2), 'i'))
        else:
            saida.append((m.group(3), 'c'))
        pos = m.end()
    if pos < len(texto):
        saida.append((texto[pos:], None))

    xml = []
    for txt, tipo in saida:
        if not txt:
            continue
        if tipo == 'c':
            rpr = ('<w:rPr>' + FONT_CODE + '<w:sz w:val="18"/><w:szCs w:val="18"/>'
                   '<w:color w:val="943634"/></w:rPr>')
        elif tipo == 'b':
            rpr = '<w:rPr>' + base + '<w:b/></w:rPr>'
        elif tipo == 'i':
            rpr = '<w:rPr>' + base + '<w:i/></w:rPr>'
        else:
            rpr = '<w:rPr>' + base + '</w:rPr>'
        xml.append('<w:r>' + rpr + '<w:t xml:space="preserve">' + esc(txt) + '</w:t></w:r>')
    return ''.join(xml)


# --------------------------------------------------------------- paragrafos
def p_body(texto):
    ppr = ('<w:pPr><w:spacing w:before="80" w:after="0" w:line="336" w:lineRule="auto"/>'
           '<w:ind w:left="113" w:firstLine="596"/><w:contextualSpacing/>'
           '<w:rPr>' + FONT_BODY + '</w:rPr></w:pPr>')
    return '<w:p>' + ppr + runs(texto) + '</w:p>'


def p_question(texto):
    rpr = FONT_BODY + '<w:b/><w:color w:val="1F3763"/><w:sz w:val="25"/><w:szCs w:val="25"/>'
    ppr = ('<w:pPr><w:keepNext/><w:keepLines/>'
           '<w:spacing w:before="360" w:after="60" w:line="276" w:lineRule="auto"/>'
           '<w:ind w:left="0" w:firstLine="0"/><w:jc w:val="left"/><w:outlineLvl w:val="2"/>'
           '<w:rPr>' + rpr + '</w:rPr></w:pPr>'
           '<w:r><w:rPr>' + rpr + '</w:rPr>'
           '<w:t xml:space="preserve">' + esc(texto) + '</w:t></w:r>')
    return '<w:p>' + ppr + '</w:p>'


def p_sub(texto):
    rpr = (FONT_BODY + '<w:b/><w:i/><w:color w:val="2F5496"/>'
           '<w:sz w:val="22"/><w:szCs w:val="22"/>')
    ppr = ('<w:pPr><w:keepNext/><w:keepLines/>'
           '<w:spacing w:before="240" w:after="40" w:line="276" w:lineRule="auto"/>'
           '<w:ind w:left="113" w:firstLine="0"/><w:jc w:val="left"/><w:outlineLvl w:val="3"/>'
           '<w:rPr>' + rpr + '</w:rPr></w:pPr>'
           '<w:r><w:rPr>' + rpr + '</w:rPr>'
           '<w:t xml:space="preserve">' + esc(texto) + '</w:t></w:r>')
    return '<w:p>' + ppr + '</w:p>'


def p_destaque(texto, rotulo='Enunciado: '):
    """Caixa com barra azul a esquerda. Usada para o enunciado copiado e para a nota de escopo."""
    rpr = FONT_BODY + '<w:i/><w:color w:val="404040"/><w:sz w:val="20"/><w:szCs w:val="20"/>'
    ppr = ('<w:pPr><w:pBdr>'
           '<w:left w:val="single" w:sz="18" w:space="8" w:color="8EAADB"/></w:pBdr>'
           '<w:shd w:val="clear" w:color="auto" w:fill="F5F7FB"/>'
           '<w:spacing w:before="40" w:after="160" w:line="264" w:lineRule="auto"/>'
           '<w:ind w:left="284" w:right="170" w:firstLine="0"/><w:jc w:val="left"/>'
           '<w:rPr>' + rpr + '</w:rPr></w:pPr>')
    corpo = ''
    if rotulo:
        corpo += ('<w:r><w:rPr>' + rpr + '<w:b/></w:rPr>'
                  '<w:t xml:space="preserve">' + esc(rotulo) + '</w:t></w:r>')
    corpo += runs(texto, rpr)
    return '<w:p>' + ppr + corpo + '</w:p>'


def p_item(texto, marcador='•'):
    ppr = ('<w:pPr><w:spacing w:before="60" w:after="0" w:line="300" w:lineRule="auto"/>'
           '<w:ind w:left="680" w:hanging="340"/><w:contextualSpacing/>'
           '<w:rPr>' + FONT_BODY + '</w:rPr></w:pPr>')
    # O tab e um elemento, nao um caractere dentro do texto: escrito como texto, o Word o
    # converte na primeira gravacao e o arquivo gerado deixa de bater com o revisado.
    prefixo = ('<w:r><w:rPr>' + FONT_BODY + '<w:b/></w:rPr>'
               '<w:t>' + esc(marcador) + '</w:t></w:r>'
               '<w:r><w:rPr>' + FONT_BODY + '<w:b/></w:rPr><w:tab/></w:r>')
    return '<w:p>' + ppr + prefixo + runs(texto) + '</w:p>'


def p_ref(texto):
    ppr = ('<w:pPr><w:spacing w:before="0" w:after="140" w:line="264" w:lineRule="auto"/>'
           '<w:ind w:left="567" w:hanging="567"/><w:jc w:val="left"/>'
           '<w:rPr>' + FONT_BODY + '<w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr></w:pPr>')
    return '<w:p>' + ppr + runs(texto) + '</w:p>'


def p_legenda(texto):
    rpr = (FONT_BODY + '<w:i/><w:iCs/><w:color w:val="404040"/>'
           '<w:sz w:val="18"/><w:szCs w:val="18"/>')
    ppr = ('<w:pPr><w:spacing w:before="60" w:after="160" w:line="264" w:lineRule="auto"/>'
           '<w:ind w:left="0" w:firstLine="0"/><w:jc w:val="center"/>'
           '<w:rPr>' + rpr + '</w:rPr></w:pPr>'
           '<w:r><w:rPr>' + rpr + '</w:rPr>'
           '<w:t xml:space="preserve">' + esc(texto) + '</w:t></w:r>')
    return '<w:p>' + ppr + '</w:p>'


def p_code_line(linha):
    rpr = FONT_CODE + '<w:sz w:val="17"/><w:szCs w:val="17"/><w:color w:val="1B1B1F"/>'
    ppr = ('<w:pPr><w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/>'
           '<w:ind w:left="0" w:right="0" w:firstLine="0"/><w:jc w:val="left"/>'
           '<w:rPr>' + rpr + '</w:rPr></w:pPr>')
    corpo = ''
    if linha:
        corpo = ('<w:r><w:rPr>' + rpr + '</w:rPr>'
                 '<w:t xml:space="preserve">' + esc(linha) + '</w:t></w:r>')
    return '<w:p>' + ppr + corpo + '</w:p>'


def bloco_codigo(linhas):
    """Bloco de codigo em tabela de uma celula, com fundo e barra a esquerda."""
    tbl = ('<w:tbl><w:tblPr><w:tblW w:w="%d" w:type="dxa"/>'
           '<w:tblInd w:w="113" w:type="dxa"/><w:tblBorders>'
           '<w:top w:val="single" w:sz="4" w:space="0" w:color="D6DCE5"/>'
           '<w:left w:val="single" w:sz="12" w:space="0" w:color="8EAADB"/>'
           '<w:bottom w:val="single" w:sz="4" w:space="0" w:color="D6DCE5"/>'
           '<w:right w:val="single" w:sz="4" w:space="0" w:color="D6DCE5"/>'
           '<w:insideH w:val="none" w:sz="0" w:space="0" w:color="auto"/>'
           '<w:insideV w:val="none" w:sz="0" w:space="0" w:color="auto"/></w:tblBorders>'
           '<w:tblCellMar><w:top w:w="113" w:type="dxa"/><w:left w:w="170" w:type="dxa"/>'
           '<w:bottom w:w="113" w:type="dxa"/><w:right w:w="113" w:type="dxa"/></w:tblCellMar>'
           '<w:tblLook w:val="0000"/></w:tblPr>'
           '<w:tblGrid><w:gridCol w:w="%d"/></w:tblGrid>' % (LARG, LARG))
    tbl += ('<w:tr><w:tc><w:tcPr><w:tcW w:w="%d" w:type="dxa"/>'
            '<w:shd w:val="clear" w:color="auto" w:fill="F7F8FA"/></w:tcPr>' % LARG)
    tbl += ''.join(p_code_line(l) for l in linhas)
    tbl += '</w:tc></w:tr></w:tbl>'
    espaco = ('<w:p><w:pPr><w:spacing w:before="0" w:after="0" w:line="120" w:lineRule="auto"/>'
              '<w:ind w:left="0" w:firstLine="0"/><w:rPr><w:sz w:val="10"/></w:rPr></w:pPr></w:p>')
    return tbl + espaco


# Larguras de coluna, em twips, medidas no documento depois do ajuste manual no Word. A chave e o
# cabecalho da tabela no markdown. Colunas iguais raramente ficam boas: "Propriedade" precisa de
# pouco espaco e "O que garante" de muito. Sem este mapa, cada geracao desfaria o ajuste.
COLUNAS = {
    ('Propriedade', 'O que garante'): (1583, 7513),
    ('Elemento', 'Papel'): (3113, 6125),
    ('Critério', 'Fila (*queue*)', 'Tópico (*topic*)'): (2473, 3607, 3158),
    ('Critério', 'Persistência tradicional', 'Event Sourcing'): (2133, 3497, 3608),
    ('Questão', 'Arquivo', 'Link'): (974, 5325, 2939),
}
LARG_TABELA = 9238  # largura padrao, para uma tabela que ainda nao esteja no mapa


def alinhamentos(linha_separadora, n):
    """Le a linha |---|:---:| do markdown: dois-pontos dos dois lados significa centralizado."""
    marcas = [c.strip() for c in linha_separadora.strip().strip('|').split('|')]
    saida = []
    for i in range(n):
        marca = marcas[i] if i < len(marcas) else '---'
        if marca.startswith(':') and marca.endswith(':'):
            saida.append('center')
        elif marca.endswith(':'):
            saida.append('right')
        else:
            saida.append('left')
    return saida


def tabela(linhas_md):
    """Tabela de grade, com a primeira linha em faixa escura e largura fixa por coluna."""
    celulas = lambda l: [c.strip() for c in l.strip().strip('|').split('|')]
    cabecalho = celulas(linhas_md[0])
    corpo_md = [celulas(l) for l in linhas_md[2:]]
    n = len(cabecalho)
    jc = alinhamentos(linhas_md[1], n)

    largura = COLUNAS.get(tuple(cabecalho))
    if not largura or len(largura) != n:
        largura = [LARG_TABELA // n] * n
        largura[-1] = LARG_TABELA - (LARG_TABELA // n) * (n - 1)
    total = sum(largura)

    def linha(vals, eh_cabecalho):
        tcs = ''.join(
            '<w:tc><w:tcPr><w:tcW w:w="%d" w:type="dxa"/>%s<w:vAlign w:val="center"/></w:tcPr>'
            '<w:p><w:pPr><w:spacing w:before="40" w:after="40" w:line="252" w:lineRule="auto"/>'
            '<w:ind w:firstLine="0"/><w:jc w:val="%s"/></w:pPr>%s</w:p></w:tc>'
            % (largura[i],
               '<w:shd w:val="clear" w:color="auto" w:fill="323E4F"/>' if eh_cabecalho else '',
               'left' if eh_cabecalho else jc[i],
               runs(v, FONT_BODY
                    + ('<w:b/><w:color w:val="FFFFFF"/>' if eh_cabecalho else '')
                    + '<w:sz w:val="19"/><w:szCs w:val="19"/>'))
            for i, v in enumerate((vals + [''] * n)[:n]))
        return '<w:tr>%s%s</w:tr>' % (
            '<w:trPr><w:tblHeader/></w:trPr>' if eh_cabecalho else '', tcs)

    return ('<w:tbl><w:tblPr><w:tblW w:w="%d" w:type="dxa"/>'
            '<w:tblInd w:w="113" w:type="dxa"/><w:tblBorders>'
            '<w:top w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>'
            '<w:left w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>'
            '<w:bottom w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>'
            '<w:right w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>'
            '<w:insideH w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>'
            '<w:insideV w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>'
            '</w:tblBorders><w:tblLayout w:type="fixed"/><w:tblCellMar>'
            '<w:top w:w="60" w:type="dxa"/><w:left w:w="113" w:type="dxa"/>'
            '<w:bottom w:w="60" w:type="dxa"/><w:right w:w="113" w:type="dxa"/>'
            '</w:tblCellMar><w:tblLook w:val="0000" w:firstRow="0" w:lastRow="0" '
            'w:firstColumn="0" w:lastColumn="0" w:noHBand="0" w:noVBand="0"/></w:tblPr>'
            '<w:tblGrid>%s</w:tblGrid>%s</w:tbl>'
            '<w:p><w:pPr><w:spacing w:before="0" w:after="0" w:line="120" w:lineRule="auto"/>'
            '<w:rPr><w:sz w:val="10"/></w:rPr></w:pPr></w:p>'
            % (total, ''.join('<w:gridCol w:w="%d"/>' % x for x in largura),
               linha(cabecalho, True) + ''.join(linha(c, False) for c in corpo_md)))


# ------------------------------------------------------------------- figuras
imagens = []   # (rid, nome no pacote, caminho de origem)
links = []     # (rid, url)
_prox = [9000]
_fig = [0]


def png_dim(caminho):
    """Largura e altura de um PNG, lidas direto do IHDR, sem depender do Pillow."""
    with open(caminho, 'rb') as f:
        cab = f.read(24)
    if len(cab) < 24 or cab[:8] != b'\x89PNG\r\n\x1a\n':
        return None
    return struct.unpack('>II', cab[16:24])


def figura(caminho_rel, legenda_txt):
    caminho = os.path.join(RAIZ, 'docs', caminho_rel.replace('/', os.sep))
    dim = png_dim(caminho) if os.path.exists(caminho) else None
    if not dim:
        sys.exit('ERRO: figura nao encontrada ou invalida: %s' % caminho)
    larg_px, alt_px = dim
    # 1 twip = 635 EMU. A largura ocupa a area util; a altura acompanha a proporcao real do
    # arquivo, porque fixar as duas de forma independente e o que deixa a figura esticada.
    cx = LARG * 635
    cy = int(round(cx * float(alt_px) / larg_px))
    _prox[0] += 1
    _fig[0] += 1
    rid = 'rId%d' % _prox[0]
    nome = 'figura%02d.png' % _fig[0]
    imagens.append((rid, nome, caminho))
    ident = 3000 + _fig[0]
    desenho = (
        '<w:p><w:pPr><w:jc w:val="center"/>'
        '<w:spacing w:before="240" w:after="0"/><w:ind w:left="0" w:firstLine="0"/></w:pPr>'
        '<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0">'
        '<wp:extent cx="%d" cy="%d"/><wp:effectExtent l="0" t="0" r="0" b="0"/>'
        '<wp:docPr id="%d" name="Figura %d"/><wp:cNvGraphicFramePr>'
        '<a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"'
        ' noChangeAspect="1"/></wp:cNvGraphicFramePr>'
        '<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">'
        '<a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
        '<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">'
        '<pic:nvPicPr><pic:cNvPr id="%d" name="%s"/><pic:cNvPicPr/></pic:nvPicPr>'
        '<pic:blipFill><a:blip r:embed="%s"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
        '<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="%d" cy="%d"/></a:xfrm>'
        '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>'
        '</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>'
        % (cx, cy, ident, _fig[0], ident, nome, rid, cx, cy))
    return desenho + p_legenda('Figura %d - %s' % (_fig[0], legenda_txt))


# --------------------------------------------------------------- markdown
def converte(markdown):
    """Percorre o relatorio e devolve o corpo do documento em WordprocessingML."""
    linhas = markdown.split('\n')
    saida = []
    i = 0
    questao = 0
    while i < len(linhas):
        linha = linhas[i]
        bruta = linha.rstrip()
        texto = bruta.strip()

        if texto.startswith('# ') or texto.startswith('---') or not texto:
            i += 1
            continue

        # identificacao do trabalho: ja esta na capa
        if texto.startswith('**Disciplina:**') or texto.startswith('**Professor:**'):
            i += 1
            continue

        # titulo de questao ou de secao
        if texto.startswith('## '):
            titulo = texto[3:].strip()
            m = re.match(r'^(\d+)\.\s+(.*)$', titulo, re.S)
            if m:
                questao = int(m.group(1))
                enunciado = m.group(2).strip()
                # o enunciado pode continuar na linha seguinte
                while i + 1 < len(linhas) and linhas[i + 1].strip() and \
                        not linhas[i + 1].startswith(('#', '-', '|', '`', '>', '!', '*')):
                    break
                saida.append(p_question('Questão %d' % questao))
                saida.append(p_destaque(enunciado))
            else:
                saida.append(p_question(titulo))
            i += 1
            continue

        if texto.startswith('### '):
            saida.append(p_sub(texto[4:].strip()))
            i += 1
            continue

        # figura
        m = re.match(r'^!\[([^\]]*)\]\(([^)]+)\)$', texto)
        if m:
            saida.append(figura(m.group(2), m.group(1)))
            i += 1
            continue

        # bloco de codigo
        if texto.startswith('```'):
            i += 1
            corpo = []
            while i < len(linhas) and not linhas[i].strip().startswith('```'):
                corpo.append(linhas[i].rstrip())
                i += 1
            i += 1
            while corpo and not corpo[-1].strip():
                corpo.pop()
            saida.append(bloco_codigo(corpo))
            continue

        # tabela
        if texto.startswith('|'):
            bloco = []
            while i < len(linhas) and linhas[i].strip().startswith('|'):
                bloco.append(linhas[i].strip())
                i += 1
            saida.append(tabela(bloco))
            continue

        # citacao: caixa destacada
        if texto.startswith('>'):
            bloco = []
            while i < len(linhas) and linhas[i].strip().startswith('>'):
                bloco.append(linhas[i].strip().lstrip('>').strip())
                i += 1
            saida.append(p_destaque(' '.join(x for x in bloco if x), rotulo=''))
            continue

        # lista com marcador ou numerada, com continuacao indentada
        m = re.match(r'^(-|\d+\.)\s+(.*)$', texto)
        if m:
            while i < len(linhas):
                atual = linhas[i].strip()
                mm = re.match(r'^(-|\d+\.)\s+(.*)$', atual)
                if not mm:
                    break
                marcador = '•' if mm.group(1) == '-' else mm.group(1)
                item = [mm.group(2)]
                i += 1
                while i < len(linhas) and linhas[i].startswith('  ') and linhas[i].strip() \
                        and not re.match(r'^(-|\d+\.)\s+', linhas[i].strip()):
                    item.append(linhas[i].strip())
                    i += 1
                saida.append(p_item(' '.join(item), marcador))
            continue

        # paragrafo comum, juntando as linhas ate a proxima em branco
        bloco = [texto]
        i += 1
        while i < len(linhas) and linhas[i].strip() and \
                not linhas[i].strip().startswith(('#', '|', '```', '>', '- ', '![')) and \
                not re.match(r'^\d+\.\s', linhas[i].strip()):
            bloco.append(linhas[i].strip())
            i += 1
        saida.append(p_body(' '.join(bloco)))

    return ''.join(saida), questao


markdown = io.open(FONTE, encoding='utf-8').read()
corpo, ultima = converte(markdown)
if ultima != 14:
    sys.exit('ERRO: esperava 14 questoes, encontrei %d' % ultima)

# ------------------------------------------------------- recorta o molde
# Mantem tudo ate o paragrafo de introducao (capa, faixa "Desenvolvimento") e retoma na faixa
# "Compartilhamento". O miolo do TP1 sai inteiro.
m_intro = re.search(r'<w:p [^>]*>(?:(?!</w:p>).)*?Este trabalho prático \(DR4-TP1\).*?</w:p>',
                    d, re.S)
if not m_intro:
    sys.exit('ERRO: paragrafo de introducao do molde nao encontrado')

i_compart = d.find('>Compartilhamento<')
if i_compart < 0:
    sys.exit('ERRO: secao de compartilhamento nao encontrada')
inicio_compart = d.rfind('<w:tbl>', 0, i_compart)

cabeca = d[:m_intro.start()]
rabo = d[inicio_compart:]

def troca_mes_da_capa(xml):
    """Troca "Agosto/2026" por "Setembro/2026" na capa.

    O Word quebrou a palavra em dois runs, "A" e "gosto/2026", então um replace direto não
    encontra nada. Para cada "gosto/2026", recuamos até o "A" solto que o precede.
    """
    alvo, novo = '<w:t>gosto/2026</w:t>', '<w:t>%s</w:t>' % MES_CAPA[1:]
    inicial, nova_inicial = '>A</w:t>', '>%s</w:t>' % MES_CAPA[0]
    while True:
        i = xml.find(alvo)
        if i < 0:
            return xml
        j = xml.rfind(inicial, 0, i)
        if j < 0:
            return xml
        xml = xml[:j] + nova_inicial + xml[j + len(inicial):]
        i = xml.find(alvo)
        xml = xml[:i] + novo + xml[i + len(alvo):]


# capa: TP1 -> TP2 e o mes da entrega
cabeca = troca_mes_da_capa(cabeca.replace('TP1', 'TP2'))

# introducao, reescrita para o TP2
cabeca += p_body(INTRO)

# ------------------------------------------------------- ajusta o fechamento
# paragrafo de compartilhamento: descrevia o TP1
m_share = re.search(
    r'<w:p [^>]*>(?:(?!</w:p>).)*?O projeto (?:de refatoração )?pode ser acessado.*?</w:p>',
    rabo, re.S)
if m_share:
    rabo = rabo[:m_share.start()] + p_body(COMPARTILHAMENTO) + rabo[m_share.end():]

# referencias: substitui as do TP1 pelas efetivamente usadas aqui
i_ref = rabo.find('>Referências Bibliográficas<')
if i_ref < 0:
    sys.exit('ERRO: secao de referencias nao encontrada')
fim_tbl_ref = rabo.find('</w:tbl>', i_ref) + len('</w:tbl>')
sect = re.search(r'<w:sectPr[^>]*>.*?</w:sectPr>', d, re.S).group(0)
rabo = (rabo[:fim_tbl_ref] + ''.join(p_ref(r) for r in REFERENCIAS)
        + sect + '</w:body></w:document>')

# links do TP1 -> links do TP2, no texto visivel
rabo = rabo.replace(
    'https://github.com/andrebecker84/ecommerce-ddd-refactoring-TP1/blob/main/docs/RELATORIO_TP1.md',
    REPO_RELATORIO)
rabo = rabo.replace('https://github.com/andrebecker84/ecommerce-ddd-refactoring-TP1', REPO)

novo = cabeca + corpo + rabo
io.open(doc_path, 'w', encoding='utf-8').write(novo)

# ------------------------------------- cabecalho, rodape e a data do documento
DIA, MES, ANO = DATA_ENTREGA.split('/')
DATA_ISO = '%s-%s-%sT00:00:00' % (ANO, MES, DIA)
for parte in ('header1', 'header2', 'header3', 'footer1', 'footer2', 'footer3'):
    caminho = os.path.join(TRAB, 'word', parte + '.xml')
    if not os.path.exists(caminho):
        continue
    x = io.open(caminho, encoding='utf-8').read()
    x = re.sub(r'\d{2}/\d{2}/2026', DATA_ENTREGA, x)
    x = re.sub(r'w:fullDate="[^"]*"', 'w:fullDate="%sZ"' % DATA_ISO, x)
    x = x.replace('TP1', 'TP2')
    io.open(caminho, 'w', encoding='utf-8').write(x)

# A data do cabecalho vive num controle ligado ao customXml. Sem trocar a fonte, o Word
# restaura a data do TP1 na primeira abertura.
custom = os.path.join(TRAB, 'customXml')
if os.path.isdir(custom):
    for nome in os.listdir(custom):
        if not nome.endswith('.xml'):
            continue
        caminho = os.path.join(custom, nome)
        x = io.open(caminho, encoding='utf-8').read()
        if 'PublishDate' not in x:
            continue
        x = re.sub(r'(<PublishDate>)[^<]*(</PublishDate>)',
                   lambda mm: mm.group(1) + DATA_ISO + mm.group(2), x)
        io.open(caminho, 'w', encoding='utf-8').write(x)

# --------------------------- relacionamentos: remove o que o novo corpo nao usa
rels_path = os.path.join(TRAB, 'word', '_rels', 'document.xml.rels')
rels = io.open(rels_path, encoding='utf-8').read()
usados = set(re.findall(r'r:(?:embed|id|link)="(rId\d+)"', novo))
mantidos, removidos = [], []
for m in re.finditer(r'<Relationship [^>]*/>', rels):
    tag = m.group(0)
    rid = re.search(r'Id="(rId\d+)"', tag).group(1)
    alvo = re.search(r'Target="([^"]+)"', tag).group(1)
    if rid not in usados and alvo.startswith('media/'):
        removidos.append(alvo)
    else:
        if 'hyperlink' in tag and 'ecommerce-ddd-refactoring-TP1' in tag:
            tag = tag.replace(
                'ecommerce-ddd-refactoring-TP1/blob/main/docs/RELATORIO_TP1.md',
                'ecommerce-ddd-domain-events-TP2/blob/main/docs/RELATORIO_TP2.md')
            tag = tag.replace('ecommerce-ddd-refactoring-TP1', 'ecommerce-ddd-domain-events-TP2')
        mantidos.append(tag)
novos = ''.join(
    '<Relationship Id="%s" Type="http://schemas.openxmlformats.org/officeDocument/'
    '2006/relationships/image" Target="media/%s"/>' % (rid, nome)
    for rid, nome, _ in imagens)
novos += ''.join(
    '<Relationship Id="%s" Type="http://schemas.openxmlformats.org/officeDocument/'
    '2006/relationships/hyperlink" Target="%s" TargetMode="External"/>'
    % (rid, url.replace('&', '&amp;'))
    for rid, url in links)
rels_novo = re.sub(r'<Relationship [^>]*/>', '', rels)
rels_novo = rels_novo.replace('</Relationships>', ''.join(mantidos) + novos + '</Relationships>')
io.open(rels_path, 'w', encoding='utf-8').write(rels_novo)
for alvo in removidos:
    caminho = os.path.join(TRAB, 'word', alvo.replace('/', os.sep))
    if os.path.exists(caminho):
        os.remove(caminho)

media = os.path.join(TRAB, 'word', 'media')
if imagens and not os.path.isdir(media):
    os.makedirs(media)
for _, nome, origem in imagens:
    shutil.copyfile(origem, os.path.join(media, nome))

# ---------------------------------------------------------------- reempacota
if os.path.exists(SAIDA):
    try:
        os.remove(SAIDA)
    except PermissionError:
        shutil.rmtree(TRAB)
        sys.exit('ERRO: %s esta aberto (Word?). Feche-o e rode de novo.' % SAIDA)
with zipfile.ZipFile(SAIDA, 'w', zipfile.ZIP_DEFLATED) as z:
    z.write(os.path.join(TRAB, '[Content_Types].xml'), '[Content_Types].xml')
    for raiz_dir, _, arqs in os.walk(TRAB):
        for a in arqs:
            full = os.path.join(raiz_dir, a)
            rel = os.path.relpath(full, TRAB).replace(os.sep, '/')
            if rel != '[Content_Types].xml':
                z.write(full, rel)
shutil.rmtree(TRAB)
print('gerado: %s (%.0f KB) | questoes: %d | figuras: %d | imagens do TP1 removidas: %d'
      % (SAIDA, os.path.getsize(SAIDA) / 1024, ultima, len(imagens), len(removidos)))
