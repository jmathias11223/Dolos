#!/bin/bash

java -cp "/home/admin/Server/bin:/home/admin/Server/bin/DB/sqlite.jar" ServerDriver 10000 20000 5 5 & disown
