#!/bin/bash

cd ../src
pyinstaller --onefile --noconsole --hidden-import=pynput.keyboard._xorg --hidden-import=pynput.mouse._xorg master.py

cd dist/
mv -f master config
mv -f config ..

cd ..
rm -f *.spec
rm -rf build/
rm -rf __pycache__/
rm -rf dist/

