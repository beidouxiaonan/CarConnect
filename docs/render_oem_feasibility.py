"""Render the authored Markdown report with inline SVG; no network dependencies."""
from pathlib import Path
import html
import re
import xml.etree.ElementTree as ET
from markdown_it import MarkdownIt
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
OUT = ROOT / 'oem-wireless-diagrams'
OUT.mkdir(exist_ok=True)
FONT = Path('C:/Windows/Fonts/msyh.ttc')
COLORS = {'normal': ('#eef5ff', '#aac2ea'), 'green': ('#e9f6ee', '#89c7a5'),
          'amber': ('#fff4e5', '#deb777'), 'muted': ('#f3f4f7', '#bbc2cf')}

class Diagram:
    counter = 0
    def __init__(self, title, width, height):
        Diagram.counter += 1
        self.marker = f'arrow-{Diagram.counter}'
        self.width, self.height = width, height
        self.parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" role="img" aria-label="{html.escape(title)}">',
                      f'<title>{html.escape(title)}</title>',
                      f'<defs><marker id="{self.marker}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0 0 L10 5 L0 10 Z" fill="#637696"/></marker></defs>',
                      f'<rect width="{width}" height="{height}" fill="#fff"/>']
        self.image = Image.new('RGB', (width, height), '#fff')
        self.draw = ImageDraw.Draw(self.image)
        self.font = ImageFont.truetype(str(FONT), 18)
        self.label_font = ImageFont.truetype(str(FONT), 16)

    def node(self, x, y, w, h, lines, kind='normal'):
        fill, border = COLORS[kind]
        self.parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="{fill}" stroke="{border}" stroke-width="1.5"/>')
        self.draw.rounded_rectangle((x, y, x+w, y+h), radius=12, fill=fill, outline=border, width=2)
        top = y + (h - 26*len(lines))/2 + 20
        for i, line in enumerate(lines):
            self.parts.append(f'<text x="{x+w/2}" y="{top+26*i}" text-anchor="middle" fill="#1d3457" font-size="18" font-family="Microsoft YaHei, Noto Sans CJK SC, sans-serif">{html.escape(line)}</text>')
            self.draw.text((x+w/2, top+26*i-2), line, font=self.font, fill='#1d3457', anchor='ms')

    def line(self, points, label='', label_at=None, dashed=False, bidirectional=False):
        coords = ' '.join(f'{x},{y}' for x, y in points)
        dash = ' stroke-dasharray="6 5"' if dashed else ''
        start = f' marker-start="url(#{self.marker})"' if bidirectional else ''
        self.parts.append(f'<polyline points="{coords}" fill="none" stroke="#637696" stroke-width="2"{dash}{start} marker-end="url(#{self.marker})"/>')
        if dashed:
            for p, q in zip(points, points[1:]):
                length = ((q[0]-p[0])**2+(q[1]-p[1])**2)**0.5
                steps = max(1, int(length/11))
                for i in range(steps):
                    a, b = i/steps, min(1, (i+0.55)/steps)
                    self.draw.line([(p[0]+(q[0]-p[0])*a, p[1]+(q[1]-p[1])*a),
                                    (p[0]+(q[0]-p[0])*b, p[1]+(q[1]-p[1])*b)], fill='#637696', width=2)
        else:
            self.draw.line(points, fill='#637696', width=2)
        p, q = points[-2:]
        import math
        angle = math.atan2(q[1]-p[1], q[0]-p[0])
        self.draw.polygon([q, (q[0]-9*math.cos(angle-0.5), q[1]-9*math.sin(angle-0.5)),
                             (q[0]-9*math.cos(angle+0.5), q[1]-9*math.sin(angle+0.5))], fill='#637696')
        if bidirectional:
            p, q = points[1], points[0]
            angle = math.atan2(q[1]-p[1], q[0]-p[0])
            self.draw.polygon([q, (q[0]-9*math.cos(angle-0.5), q[1]-9*math.sin(angle-0.5)),
                                 (q[0]-9*math.cos(angle+0.5), q[1]-9*math.sin(angle+0.5))], fill='#637696')
        if label:
            x, y = label_at
            width = self.draw.textlength(label, font=self.label_font)
            self.parts.append(f'<rect x="{x-width/2-3}" y="{y-17}" width="{width+6}" height="23" fill="white"/>')
            self.parts.append(f'<text x="{x}" y="{y}" text-anchor="middle" fill="#556784" font-size="16" font-family="Microsoft YaHei, Noto Sans CJK SC, sans-serif">{html.escape(label)}</text>')
            box = self.draw.textbbox((x, y), label, font=self.label_font, anchor='ms')
            self.draw.rectangle((box[0]-3, box[1]-2, box[2]+3, box[3]+2), fill='#fff')
            self.draw.text((x, y), label, font=self.label_font, fill='#556784', anchor='ms')

    def save(self, name):
        svg = '\n'.join(self.parts + ['</svg>'])
        ET.fromstring(svg)
        (OUT / f'{name}.svg').write_text(svg, encoding='utf-8')
        self.image.save(OUT / f'{name}.png')
        return svg

a = Diagram('原车蓝牙接入与 Wi-Fi 音视频架构', 1000, 510)
a.node(55, 25, 270, 70, ['标准 Android 蓝牙 / t3'], 'muted')
a.node(55, 195, 270, 90, ['CarConnect 普通 APK', '无需替换系统蓝牙'])
a.node(405, 60, 270, 80, ['BC03 原车蓝牙服务', '状态、手机与模块地址'])
a.node(405, 250, 270, 80, ['gocsdk 原厂守护进程', '既有串口与 SPP socket'])
a.node(740, 150, 225, 85, ['外接原车蓝牙模块', '与标准栈可能独立'])
a.node(740, 385, 225, 80, ['iPhone', '授权并加入热点'], 'green')
a.line([(190, 95), (190, 195)], '另一条路径', (245, 150), True)
a.line([(325, 217), (365, 217), (365, 100), (405, 100)], 'Binder', (365, 170))
a.line([(675, 100), (852, 100), (852, 150)])
a.line([(325, 262), (365, 262), (365, 290), (405, 290)], '核对后接入', (364, 350))
a.line([(675, 290), (852, 290), (852, 235)], 'SPP 通道', (758, 280))
a.line([(852, 235), (920, 235), (920, 385)], 'iAP2 接入', (920, 340))
a.line([(190, 285), (190, 425), (740, 425)], 'Wi-Fi：CarPlay 音视频与控制', (465, 415), bidirectional=True)
architecture = a.save('architecture')

b = Diagram('SPP 清理与冷却流程：只有确认释放才接入', 1000, 875)
b.node(70, 30, 550, 70, ['核对文件、目标手机和本应用通道归属'])
b.node(70, 155, 550, 70, ['读取 SPP：空闲直接继续；占用才考虑清理'])
b.node(70, 280, 550, 70, ['占用且开关开启：若冷却未结束，等待剩余时间', 'C0：同一任务，不重复发 VH / VF'], 'amber')
b.node(70, 405, 550, 70, ['Binder SppDisConnect 请求', 'C2：观察最多 5 秒，不把返回当成功'])
b.node(70, 530, 550, 70, ['仍占用：一次已核对的底层 VH 请求', 'C4：观察最多 12 秒，写出不等于释放'], 'amber')
b.node(70, 745, 550, 85, ['稳定释放后重新核对，继续 VF / iAP2 接入', 'C3 / C5：未连接状态连续稳定 300ms'], 'green')
b.node(735, 410, 215, 180, ['清理关闭、取消、', '超时或再次占用', '', '停止本轮', '不发送新 VF'], 'muted')
for y1, y2 in [(100, 155), (225, 280), (350, 405), (475, 530)]:
    b.line([(345, y1), (345, y2)])
b.line([(345, 600), (345, 745)], '释放稳定才继续', (345, 685))
b.line([(620, 190), (675, 190), (675, 786), (620, 786)], '空闲', (675, 718))
b.line([(70, 440), (28, 440), (28, 786), (70, 786)], '稳定释放', (73, 696))
b.line([(620, 190), (845, 190), (845, 410)], '占用且清理关闭', (845, 310))
b.line([(620, 565), (735, 565)], '未释放', (676, 552))
cleanup = b.save('spp-flow')

c = Diagram('无线无首帧有限恢复', 1000, 485)
c.node(35, 45, 240, 100, ['当前无线控制已启动', '自动连接与恢复开启'])
c.node(355, 45, 285, 100, ['前台且 Surface 有效', '连续 60 秒计时'])
c.node(720, 45, 240, 100, ['仍没有真实首帧', '检查本轮恢复次数'], 'amber')
c.node(355, 265, 285, 95, ['关闭本应用会话', '按原 15～60 秒退避重试'])
c.node(720, 265, 240, 95, ['已自动恢复两次', '保持监听并提示检查'], 'muted')
c.node(35, 265, 240, 95, ['真实首帧出现', '取消计时，保持会话'], 'green')
c.line([(275, 95), (355, 95)])
c.line([(640, 95), (720, 95)], '超时', (680, 81))
c.line([(840, 145), (840, 265)], '达到两次', (890, 215))
c.line([(720, 110), (690, 110), (690, 312), (640, 312)], '未满两次', (690, 233))
# Recovery resumes at the upper start; keep the returning line outside the success node.
c.line([(355, 312), (300, 312), (300, 180), (155, 180), (155, 145)], '新会话', (234, 171), True)
c.line([(497, 145), (497, 215), (155, 215), (155, 265)], '出现首帧', (295, 204))
first_frame = c.save('first-frame')

diagrams = iter([architecture, cleanup, first_frame])
md = MarkdownIt('commonmark', {'html': False}).enable('table')
original_fence = md.renderer.rules.get('fence')
def fence(tokens, idx, options, env):
    token = tokens[idx]
    if token.info.strip() == 'mermaid':
        return '<figure class="diagram">' + next(diagrams) + '</figure>\n'
    return original_fence(tokens, idx, options, env)
md.renderer.rules['fence'] = fence
source = (ROOT / 'OEM-WIRELESS-FEASIBILITY.zh-CN.md').read_text(encoding='utf-8')
body = md.render(source)
# Use public equivalents for source references so the standalone download works.
body = body.replace('../easyplay-first-frame/', 'https://github.com/beidouxiaonan/CarConnect/blob/codex/oem014-spp-cleanup/easyplay-first-frame/')
toc = []
def headings(match):
    content = match.group(1)
    anchor = 'part-' + str(len(toc)+1)
    toc.append(f'<a href="#{anchor}">{content}</a>')
    return f'<h2 id="{anchor}">{content}</h2>'
body = re.sub(r'<h2>(.*?)</h2>', headings, body)
page = '''<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>CarConnect 原车蓝牙无线接入方案 · 可行性与适配清单</title>
<style>
:root{font-family:"Microsoft YaHei",system-ui,sans-serif;color:#20334b;background:#f2f5f9;line-height:1.85}
*{box-sizing:border-box}body{margin:0}header{background:#132b49;color:#fff;padding:32px max(24px,calc((100vw - 1350px)/2))}
.eyebrow{color:#afc8e7;font-size:13px;letter-spacing:2px}.lead{font-size:24px;font-weight:700;margin:6px 0}
.badges{display:flex;flex-wrap:wrap;gap:10px;margin-top:14px}.badges span{border:1px solid #55789c;border-radius:20px;padding:3px 13px;font-size:13px}
.layout{max-width:1350px;margin:24px auto;display:grid;grid-template-columns:250px minmax(0,1fr);gap:24px;padding:0 22px}
nav{position:sticky;top:20px;height:fit-content;font-size:13px;padding:18px;background:#fff;border-radius:12px;border:1px solid #dde5ef}
nav strong{display:block;margin-bottom:10px}nav a{display:block;color:#526a87;padding:7px 0;text-decoration:none;line-height:1.6}
main{min-width:0;background:#fff;padding:34px 38px;border:1px solid #dde5ef;border-radius:12px}h1{font-size:29px;line-height:1.4;margin-top:0}h2{font-size:23px;margin:44px 0 18px;scroll-margin-top:20px;border-top:1px solid #dee6ef;padding-top:24px}h3{font-size:18px;margin-top:24px}
p{margin:14px 0}a{color:#1760ac;text-underline-offset:3px}strong{color:#142b46}code{background:#edf2f8;border-radius:4px;padding:1px 4px;font-size:.88em;overflow-wrap:anywhere}
table{border-collapse:collapse;width:100%;font-size:13px;line-height:1.7;margin:20px 0;table-layout:fixed}th,td{border:1px solid #dce4ee;padding:10px 12px;text-align:left;vertical-align:top;overflow-wrap:anywhere}th{background:#eaf0f7;color:#213e61}tbody tr:nth-child(even){background:#f8fafc}th:first-child{width:24%}
li{padding-bottom:7px}.diagram{margin:24px 0;padding:8px;border:1px solid #dce4ee;border-radius:10px;background:white}.diagram svg{display:block;width:100%;height:auto}footer{text-align:center;color:#73839b;font-size:12px;padding:15px 20px 40px}
@media(max-width:850px){.layout{display:block;padding:0 12px}nav{position:relative;top:0;margin-bottom:18px}main{padding:24px 18px}h1{font-size:24px}table{font-size:12px}th,td{padding:7px 6px}}
@media print{body{background:white}header{background:white;color:#20334b;padding:12px}.eyebrow,.badges,nav,footer{display:none}.layout{display:block;margin:0;padding:0}main{border:0;padding:0}.diagram,table{break-inside:avoid}h2,h3{break-after:avoid}a{color:inherit}}
</style></head><body>
<header><div class="eyebrow">CARCONNECT / OEM WIRELESS</div><div class="lead">原车蓝牙无线接入方案</div><div>可行性 · 连接流程 · 安卓范围 · 蓝牙服务 · 文件清单</div><div class="badges"><span>当前目标：BC03 1.7.9</span><span>OEM 0.1.9 测试</span><span>普通 APK / 无 Root</span><span>实车验收待完成</span></div></header>
<div class="layout"><nav><strong>文档目录</strong>__TOC__</nav><main>__BODY__</main></div>
<footer>2026-10-09 · 文档和图形可离线查看；外部参考链接需要联网。</footer></body></html>'''.replace('__BODY__', body).replace('__TOC__', ''.join(toc))
target = ROOT / 'OEM-WIRELESS-FEASIBILITY.zh-CN.html'
target.write_text(page, encoding='utf-8')
assert len(toc) == 9
assert page.count('<svg ') == 3
assert 'cdn.' not in page and '<script' not in page
assert 'BC03 1.7.9' in page and 'minSdk' in page and 'Bluetooth.apk' in page
print(f'Rendered: {target}; 9 sections, 3 inline SVG diagrams, no external runtime dependencies')
