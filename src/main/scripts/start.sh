JMX_PARAMS=""
JMX_PARAMS="$JMX_PARAMS -Dcom.sun.management.jmxremote.port=12345"
JMX_PARAMS="$JMX_PARAMS -Dcom.sun.management.jmxremote.rmi.port=12345"
JMX_PARAMS="$JMX_PARAMS -Dcom.sun.management.jmxremote.authenticate=false"
JMX_PARAMS="$JMX_PARAMS -Dcom.sun.management.jmxremote.ssl=false"
JMX_PARAMS="$JMX_PARAMS -Djava.rmi.server.hostname=$EXTERNAL_HOST_IP"

java -cp "./target/packaged/lib/*" $JMX_PARAMS $ADDITIONAL_JAVA_OPTS fuud.Main