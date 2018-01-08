#!/bin/sh
DIR=`dirname $0`
$DIR/java -XX:+UnlockExperimentalVMOptions \
    -XX:+UseCGroupMemoryLimitForHeap \
    --upgrade-module-path=/opt/upgrade-modules \
    -m com.example $@
