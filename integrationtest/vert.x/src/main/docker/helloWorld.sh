#!/bin/sh
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Copyright The original authors
#
#  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
#

DIR=`dirname $0`
$DIR/java -XX:+UnlockExperimentalVMOptions \
    -XX:+UseCGroupMemoryLimitForHeap \
    --upgrade-module-path=/opt/upgrade-modules \
    -m com.example $@
