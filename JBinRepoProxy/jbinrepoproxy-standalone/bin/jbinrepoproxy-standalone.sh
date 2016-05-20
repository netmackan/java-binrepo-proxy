#!/bin/bash

# Find directory for this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink -f "$SOURCE")"; done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# Simply run the JAR
java -jar ${DIR}/../target/jbinrepoproxy-standalone.jar $@
