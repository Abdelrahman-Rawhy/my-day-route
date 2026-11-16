import re,base64,io,subprocess,sys
s=open('www/app.html',encoding='utf8').read()
s=re.sub(r"@font-face\{[^}]*\}\n?","",s)
s=s.replace('<link rel="apple-touch-icon" href="logo.png">','<link rel="apple-touch-icon" href="logo.png">\n<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Amiri:wght@700&display=swap" rel="stylesheet">',1)
from PIL import Image
im=Image.open('www/logo.png').convert('RGBA').resize((192,192),Image.LANCZOS);b=io.BytesIO();im.save(b,'PNG',optimize=True)
uri='data:image/png;base64,'+base64.b64encode(b.getvalue()).decode()
s=s.replace('"logo.png"','"'+uri+'"')
open('/home/claude/day-route.html','w',encoding='utf8').write(s);print(len(s))
