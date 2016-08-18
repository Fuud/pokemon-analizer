JMX_PARAMS="-Dcom.sun.management.jmxremote.port=12345 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

java -cp "./target/packaged/lib/*" $JMX_PARAMS $ADDITIONAL_JAVA_OPTS fuud.Main