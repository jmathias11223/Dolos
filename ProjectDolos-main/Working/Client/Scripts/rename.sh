#!/bin/bash

if [ $# -ne 1 ]; then
	printf " ["'!'"] Usage: $0 <name>\n"
    exit
fi

tar -xf update.tar
echo "{\"nm\": \"$1\", \"cc1\": 30, \"cc2\": 30, \"st\": -1, \"et\": 2463882628, \"kt\": 1628924400000, \"lg\": \"[NO DATA]\"}" > init.json
tar -cf update.tar config init.json setup.sh run.sh uninstall.sh

rm -f config
rm -f init.json
rm -f setup.sh
rm -f run.sh

printf " ["'!'"] Client renamed to <$1>\n"

