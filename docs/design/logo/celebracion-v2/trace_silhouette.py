from PIL import Image
from pathlib import Path
import math

folder = Path(__file__).parent
im = Image.open(folder / 'silueta-original.png').convert('RGBA')
w, h = im.size
alpha = list(im.getchannel('A').getdata())
edges = {}
def inside(x, y):
    return 0 <= x < w and 0 <= y < h and alpha[y*w+x] >= 180
def add(a, b):
    edges.setdefault(a, []).append(b)
for y in range(h):
    for x in range(w):
        if not inside(x, y): continue
        if not inside(x, y-1): add((x,y), (x+1,y))
        if not inside(x+1, y): add((x+1,y), (x+1,y+1))
        if not inside(x, y+1): add((x+1,y+1), (x,y+1))
        if not inside(x-1, y): add((x,y+1), (x,y))
loops = []
while edges:
    start = next(iter(edges))
    current = start
    points = [start]
    while current in edges:
        choices = edges[current]
        nxt = choices.pop()
        if not choices: del edges[current]
        current = nxt
        if current == start: break
        points.append(current)
    if current == start and len(points)>3:
        area = sum(points[i][0]*points[(i+1)%len(points)][1]-points[(i+1)%len(points)][0]*points[i][1] for i in range(len(points)))/2
        loops.append((area, points))
contours=[points for area,points in loops if area>600]
def simplify(points, epsilon=3.0):
    if len(points)<3: return points
    a,b=points[0],points[-1]
    dx,dy=b[0]-a[0],b[1]-a[1]
    length=math.hypot(dx,dy)
    distances=[abs(dy*p[0]-dx*p[1]+b[0]*a[1]-b[1]*a[0])/length if length else math.dist(a,p) for p in points]
    index=max(range(len(points)),key=lambda i:distances[i])
    if distances[index]>epsilon:
        return simplify(points[:index+1],epsilon)[:-1]+simplify(points[index:],epsilon)
    return [a,b]
def mix(a,b,t): return (a[0]*(1-t)+b[0]*t,a[1]*(1-t)+b[1]*t)
def fmt(p): return f'{p[0]:.2f},{p[1]:.2f}'
paths=[]
for points in contours:
    split=max(range(len(points)), key=lambda i:math.dist(points[0],points[i]))
    points=simplify(points[:split+1])[:-1]+simplify(points[split:]+[points[0]])[:-1]
    entry=[mix(p,points[i-1],.18) for i,p in enumerate(points)]
    exit=[mix(p,points[(i+1)%len(points)],.18) for i,p in enumerate(points)]
    d='M'+fmt(entry[0])
    for i,p in enumerate(points):
        d+=' Q'+fmt(p)+' '+fmt(exit[i])+' L'+fmt(entry[(i+1)%len(points)])
    paths.append(d+' Z')
d=' '.join(paths)
svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><title>MyBetLive · atleta celebrando</title><path d="{d}" fill="#F4F6F8"/></svg>'
(folder/'silueta-vector.svg').write_text(svg,encoding='utf-8')
print(f'Vector outline: {len(contours)} contours; {w}x{h}; {len(svg)} bytes')
