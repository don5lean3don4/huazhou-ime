# -*- coding: utf-8 -*-
"""把 classes.dex 塞进 aapt2 产出的 APK，保证 STORED 压缩方式正确。"""
import sys, zipfile, shutil, os

apk, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]
with zipfile.ZipFile(apk) as zin, zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        zout.writestr(item, zin.read(item.filename))
    zout.write(dex, 'classes.dex', compress_type=zipfile.ZIP_DEFLATED)
print('packed', out, os.path.getsize(out))
