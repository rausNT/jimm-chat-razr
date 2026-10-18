"""Jimm Chat packager; changed 2026-10-18. GPL-2.0-or-later; no warranty."""
from pathlib import Path
import zipfile, hashlib, json, struct, re
p=Path(__file__).resolve().parent
variant=(p/'VARIANT.txt').read_text().strip()
manifest='Manifest-Version: 1.0\r\nMIDlet-Name: Jimm Chat\r\nMIDlet-Version: 0.2.0\r\nMIDlet-Vendor: Jimm Project\r\nMIDlet-1: Jimm Chat,/icon.png,jimm.Jimm\r\nMicroEdition-Configuration: CLDC-1.0\r\nMicroEdition-Profile: MIDP-2.0\r\nJimm-Version: 0.5.1\r\nMIDlet-Permissions: javax.microedition.io.Connector.socket,\r\n javax.microedition.io.Connector.http\r\n\r\n'
def utf(s):
 raw=s.encode('utf-16-be');out=bytearray()
 for i in range(0,len(raw),2):
  c=int.from_bytes(raw[i:i+2],'big')
  if 1<=c<=127:out.append(c)
  elif c<=2047:out.extend((0xc0|(c>>6),0x80|(c&63)))
  else:out.extend((0xe0|(c>>12),0x80|((c>>6)&63),0x80|(c&63)))
 return struct.pack('>H',len(out))+out
pairs=[]
for line in (p/'src/lng/RU.lang').read_text(encoding='utf-8').splitlines():
 if not line.strip().startswith('"'):continue
 m=re.match(r'\s*("(?:[^"\\]|\\.)*")\s+("(?:[^"\\]|\\.)*")',line)
 if not m:raise ValueError('Invalid language line')
 key,value=(json.loads(x) for x in m.groups())
 pairs.append((key,value.replace('###TARGET###','MOTOROLA').replace('###MODULES###','Jimm Chat')))
assert dict(pairs)['options_account']=='Учетная запись'
language=struct.pack('>H',len(pairs))+b''.join(utf(k)+utf(v) for k,v in pairs)
dest=p/'dist/JimmChat.jar'
def put(z,name,data):
 info=zipfile.ZipInfo(name,(2026,10,18,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,data)
with zipfile.ZipFile(dest,'w') as z:
 put(z,'META-INF/MANIFEST.MF',manifest.encode())
 with zipfile.ZipFile(p/'.build/preverified.jar') as src:
  for name in sorted(src.namelist()):
   if name.endswith('.class'):
    assert not name.startswith(('com/motorola/','javax/'))
    b=src.read(name);assert b[4:8]==bytes.fromhex('0003002d');put(z,name,b)
 for f in sorted((p/'resources').rglob('*')):
  if f.is_file():put(z,f.relative_to(p/'resources').as_posix(),f.read_bytes())
 put(z,'RU.lng',language);put(z,'langlist.lng',struct.pack('>H',1)+utf('RU'))
 for name in ['COPYING','NOTICE.txt','CHANGES.md']:put(z,name,(p/name).read_bytes())
b=dest.read_bytes()
(p/'dist/JimmChat.jad').write_bytes((manifest.strip()+'\r\nMIDlet-Jar-URL: JimmChat.jar\r\nMIDlet-Jar-Size: '+str(len(b))+'\r\n').encode())
report={'version':'0.2.0','variant':variant,'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'device_verified':False,'private_credentials':variant=='private','license':'GPL-2.0-or-later','license_in_jar':True}
(p/'dist/build-verification.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print(json.dumps(report))
