"""Generate non-personal TIFF fixtures and test the host build of piccodec.
Requires Pillow: cmake -S app/src/main/cpp -B build-host && cmake --build build-host -j2
python tests/native_codec_test.py build-host/piccodec
"""
import sys,tempfile,subprocess
from pathlib import Path
from PIL import Image,TiffImagePlugin
codec=str(Path(sys.argv[1]).resolve())
with tempfile.TemporaryDirectory() as work:
 root=Path(work);rgb=Image.new('RGB',(48,32));rgb.putdata([(x*5,y*7,50) for y in range(32) for x in range(48)])
 cases=[('RGB','raw'),('RGB','tiff_lzw'),('RGB','tiff_adobe_deflate'),('L','raw'),('L','packbits'),('P','tiff_lzw'),('CMYK','raw')]
 for mode,compression in cases:
  im=rgb.convert(mode);src=root/(mode+compression+'.tif');out=src.with_suffix('.ppm');im.save(src,compression=compression)
  subprocess.run([codec,str(src),'2',str(out)],check=True,capture_output=True)
  decoded=Image.open(out);assert decoded.size==im.size,(mode,compression,decoded.size)
  if mode in ('RGB','L'):assert list(decoded.getdata())==list(im.convert('RGB').getdata()),(mode,compression)
  print('PASS',mode,compression)
 im=Image.new('I;16',(48,32));im.putdata([i*31%65536 for i in range(48*32)]);src=root/'gray16.tif';im.save(src);subprocess.run([codec,str(src),'2',str(src.with_suffix('.ppm'))],check=True,capture_output=True);print('PASS gray16 decode')
 bad=root/'broken.tif';bad.write_bytes(b'II*\x00\xff'*50);r=subprocess.run([codec,str(bad),'2',str(root/'bad.ppm')],capture_output=True);assert r.returncode==3;print('PASS malformed input rejected')
 for orientation in range(1,9):
  src=root/f'orient{orientation}.tif';tag=TiffImagePlugin.ImageFileDirectory_v2();tag[274]=orientation;rgb.save(src,tiffinfo=tag);out=src.with_suffix('.ppm');r=subprocess.run([codec,str(src),'2',str(out)],capture_output=True)
  if r.returncode: print('UNSUPPORTED orientation',orientation);continue
  from PIL import ImageOps
  expected=ImageOps.exif_transpose(Image.open(src));actual=Image.open(out)
  assert actual.size==expected.size,(orientation,actual.size,expected.size)
  assert list(actual.getdata())==list(expected.getdata()),orientation
  print('PASS orientation',orientation)
