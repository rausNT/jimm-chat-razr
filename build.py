"""Jimm Chat build, created 2026-10-18. GPL-2.0-or-later; no warranty."""
from pathlib import Path
import os, sys, shutil, subprocess, hashlib, json, urllib.request, zipfile
ROOT=Path(__file__).resolve().parent
os.chdir(ROOT)
def run(args): subprocess.run([str(a) for a in args],check=True)
def jdk_tool(name):
    suffix='.exe' if os.name=='nt' else ''
    if os.environ.get('JAVA_HOME'):
        candidate=Path(os.environ['JAVA_HOME'])/'bin'/(name+suffix)
        if candidate.exists(): return candidate
    candidate=shutil.which(name)
    if candidate:return candidate
    raise SystemExit('Install JDK 17 and set JAVA_HOME or put its bin directory on PATH.')
java=jdk_tool('java')
deps=ROOT/'.deps';deps.mkdir(exist_ok=True)
for dep in json.loads((ROOT/'dependencies.json').read_text(encoding='utf-8')):
    target=deps/dep['name']
    local=Path(os.environ.get('JIMM_TOOLS',str(ROOT.parent/'java-me-tools')))/dep['name']
    if not target.exists():
        if local.exists():shutil.copyfile(local,target)
        else:
            if '--offline' in sys.argv:raise SystemExit('Missing dependency: '+dep['name'])
            print('Downloading build dependency:',dep['name'])
            with urllib.request.urlopen(dep['url'],timeout=60) as response:target.write_bytes(response.read())
    if hashlib.sha256(target.read_bytes()).hexdigest()!=dep['sha256'].lower():raise SystemExit('Dependency hash mismatch: '+dep['name'])
work=ROOT/'.build'
if work.exists():shutil.rmtree(work)  # Only disposable output under this project.
(work/'classes').mkdir(parents=True);(work/'api').mkdir()
(ROOT/'dist').mkdir(exist_ok=True)
baseapi=os.pathsep.join(str(deps/n) for n in ['cldcapi11-2.0.4.jar','midpapi20-2.0.4.jar'])
ecj=[java,'-jar',deps/'ecj-4.6.1.jar','-source','1.3','-target','1.1','-encoding','UTF-8','-g:none','-nowarn']
run(ecj+['-bootclasspath',baseapi,'-d',work/'api',ROOT/'api-stubs'])
def archive_classes(folder,out):
    with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
        for f in sorted(folder.rglob('*.class')):z.write(f,f.relative_to(folder).as_posix())
archive_classes(work/'api',work/'motorola-api.jar')
api=baseapi+os.pathsep+str(work/'motorola-api.jar')
run(ecj+['-bootclasspath',api,'-d',work/'classes',ROOT/'src'])
archive_classes(work/'classes',work/'unverified.jar')
run([java,'-jar',deps/'proguard-base-6.2.2.jar','-injars',work/'unverified.jar','-outjars',work/'preverified.jar','-libraryjars',api,'-dontoptimize','-microedition','-target','1.1','-keep','public class jimm.Jimm { public *; }','-printmapping',ROOT/'dist/mapping.txt'])
run([sys.executable,ROOT/'package.py'])
