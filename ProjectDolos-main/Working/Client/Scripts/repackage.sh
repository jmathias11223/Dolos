#!/bin/bash

cp ../src/config .
cp ../init.json .

touch update.tar
tar -cf update.tar config init.json setup.sh run.sh uninstall.sh

mv update.tar ../../Production/Client/
cp install.sh ../../Production/Client/
cp uninstall.sh ../../Production/Client/
cp rename.sh ../../Production/Client/

rm config init.json

