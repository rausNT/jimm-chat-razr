"""Offline UI tests, created 2026-10-18. GPL-2.0-or-later; no warranty."""
from pathlib import Path
import subprocess,sys,os,shutil
p=Path(__file__).resolve().parent;os.chdir(p)
jdk=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else None
def tool(n):return str(jdk/(n+('.exe' if os.name=='nt' else ''))) if jdk else shutil.which(n)
subprocess.run([sys.executable,'tests/make-stubs.py'],check=True)
sources=[str(f) for f in sorted((p/'tests/stubs').rglob('*.java'))]+[str(p/'tests/ChatTest.java'),str(p/'tests/RetryPolicyTest.java')]
subprocess.run([tool('javac'),'-encoding','UTF-8','-cp',os.pathsep.join(['.build/classes','.deps/midpapi20-2.0.4.jar']),'-d','tests/classes']+sources,check=True)
r=subprocess.run([tool('java'),'-Djava.awt.headless=true','-Xverify:all','-cp',os.pathsep.join(['tests/classes','.build/classes','.']),'jimm.ChatTest'],capture_output=True)
(p/'dist/test-result.txt').write_bytes(r.stdout+r.stderr)
print(r.stdout.decode('utf-8',errors='replace'));print(r.stderr.decode('utf-8',errors='replace'))
if r.returncode:sys.exit(r.returncode)
subprocess.run([tool('java'),'-Xverify:all','-cp',os.pathsep.join(['tests/classes','.build/classes']),'jimm.RetryPolicyTest'],check=True)
