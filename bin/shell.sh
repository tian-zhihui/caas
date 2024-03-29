#!/bin/sh
export JAVA_EXE=java
export JAVA_OPTS=-Xmx1024m 

## In case you need to remotely debug CAAS itself
# export JAVA_OPTS=$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:9302,server=y,suspend=y 

$JAVA_EXE $JAVA_OPTS -cp ../classes;../caas.jar org.kisst.cordys.caas.main.CaasMain -c ../config/caas.conf -v shell $* 

