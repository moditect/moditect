#
#  SPDX-License-Identifier: Apache-2.0
#
#   Copyright The original authors
#
#   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
#

FROM centos:7

COPY maven/target/jlink-image /opt/vertx-helloworld/
COPY maven/helloWorld.sh /opt/vertx-helloworld/bin

ADD entrypoint.sh /entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]

