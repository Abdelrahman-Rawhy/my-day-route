# v127: run before every commit — APP_VERSION in www/app.html, update/app.html and update/version.json "v" must match
import json,re,sys
a=re.search(r'const APP_VERSION=(\d+);',open('www/app.html').read()).group(1)
b=re.search(r'const APP_VERSION=(\d+);',open('update/app.html').read()).group(1)
v=str(json.load(open('update/version.json'))['v'])
same=open('www/app.html').read()==open('update/app.html').read()
print('www',a,'update',b,'json',v,'identical',same)
if not (a==b==v and same): sys.exit(1)
# v137: also refuse to ship a syntax error (a broken script means a blank app on every phone)
import subprocess,tempfile
sc=max(re.findall(r'<script>(.*?)</script>',open('www/app.html').read(),re.S),key=len)
f=tempfile.NamedTemporaryFile('w',suffix='.js',delete=False);f.write(sc);f.close()
r=subprocess.run(['node','--check',f.name],capture_output=True,text=True)
print('syntax', 'OK' if r.returncode==0 else r.stderr[:400]);sys.exit(r.returncode)
