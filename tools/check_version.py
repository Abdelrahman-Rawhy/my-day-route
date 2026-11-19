# v127: run before every commit — APP_VERSION in www/app.html, update/app.html and update/version.json "v" must match
import json,re,sys
a=re.search(r'const APP_VERSION=(\d+);',open('www/app.html').read()).group(1)
b=re.search(r'const APP_VERSION=(\d+);',open('update/app.html').read()).group(1)
v=str(json.load(open('update/version.json'))['v'])
same=open('www/app.html').read()==open('update/app.html').read()
print('www',a,'update',b,'json',v,'identical',same)
sys.exit(0 if a==b==v and same else 1)
