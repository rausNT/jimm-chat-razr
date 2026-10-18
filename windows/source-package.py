"""Package public corresponding source for embedded/exported bundles. GPL-2.0-or-later, 2026-10-18."""
from pathlib import Path
import zipfile,re
p=Path(__file__).resolve().parent.parent
assert (p/'VARIANT.txt').read_text().strip()=='public'
options=(p/'src/jimm/Options.java').read_text(encoding='utf-8')
for key in ['OPTION_UIN1','OPTION_PASSWORD1']:
    assert re.search(r'setString\s*\(Options\.'+key+r',\s*""\)',options), 'Only public defaults may be bundled'
assert not (p/'resources/personal.dat').exists(), 'Refusing to embed personal configuration in public EXE'
out=p/'windows/out';out.mkdir(exist_ok=True)
with zipfile.ZipFile(out/'source.zip','w',zipfile.ZIP_DEFLATED) as z:
    for name in ['COPYING','NOTICE.txt','CHANGES.md','BUILD.md','RESOURCES.md','dependencies.json','VARIANT.txt','build.py','build.ps1','package.py','test.py','README.md']:
        z.write(p/name,name)
    for folder in ['src','resources','api-stubs','windows']:
        for f in sorted((p/folder).rglob('*')):
            if not f.is_file() or any(part in ['out','__pycache__'] for part in f.relative_to(p).parts):continue
            z.write(f,f.relative_to(p).as_posix())
    for name in ['tests/make-stubs.py','tests/ChatTest.java','tests/ConnectionTraceTest.java']:z.write(p/name,name)
