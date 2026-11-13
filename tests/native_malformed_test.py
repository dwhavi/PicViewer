import random,subprocess,tempfile,json,time,resource,os
from pathlib import Path
import sys
r=random.Random(814);results=[];codec=str(Path(sys.argv[1]).resolve())
def limits():
 resource.setrlimit(resource.RLIMIT_AS,(256*1024*1024,)*2);resource.setrlimit(resource.RLIMIT_CPU,(1,1))
with tempfile.TemporaryDirectory() as d:
 root=Path(d)
 for i in range(300):
  payload=bytearray(r.randbytes(r.randint(0,8192)))
  if i%4==0:payload[:8]=b'II*\0'+r.randbytes(4)
  elif i%4==1:payload[:8]=b'MM\0*'+r.randbytes(4)
  elif i%4==2:payload[:12]=b'II*\0\x08\0\0\0\x01\0\x00\x01'
  src=root/'sample.bin';src.write_bytes(payload)
  for kind in (2,0):
   try:
    p=subprocess.run([codec,str(src),str(kind),str(root/'out.ppm')],capture_output=True,timeout=2,preexec_fn=limits)
    if p.returncode not in (0,3):results.append(dict(case=i,kind=kind,returncode=p.returncode,error=p.stderr[:300].decode(errors='replace')))
   except subprocess.TimeoutExpired:results.append(dict(case=i,kind=kind,error='timeout'))
print(json.dumps(dict(calls=600,unexpected=results),indent=2))
assert not results, results
