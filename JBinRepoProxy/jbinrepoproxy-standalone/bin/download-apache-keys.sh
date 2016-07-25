#!/bin/bash

# Find directory for this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink -f "$SOURCE")"; done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# XXX: This is not optimal and downloads more than needed
wget --relative --no-parent --accept KEYS --accept-regex="^https:\/\/www\.apache\.org\/dist\/[a-zA-Z0-9]+\/" -r -l 2 https://www.apache.org/dist/

mv -b www.apache.org "${DIR}/../trust/"
