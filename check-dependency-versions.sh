#!/bin/bash

( cd devtools && mvn install -PskipQuality )
echo "==============================="
echo "===- CHECKING DEPENDENCIES -==="
echo "vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv"
mvn versions:display-dependency-updates versions:display-plugin-updates | grep -E '( -> | Building )'
mvn -Ddependency-check.skip=false dependency-check:check

echo "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^"
echo "===-         DONE!         -==="
echo "==============================="
