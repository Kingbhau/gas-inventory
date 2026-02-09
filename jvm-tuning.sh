#!/bin/bash

# ============================================
# JVM PERFORMANCE TUNING SCRIPT
# ============================================
# This script configures Java VM for optimal performance
# Specifically tuned for 10-15 concurrent users with fast response times
#
# Run this as: bash jvm-tuning.sh

# ============================================
# OPTIMAL JVM PARAMETERS
# ============================================

# For 2GB RAM machine:
export JAVA_OPTS="\
  -server \
  -Xms1024m \
  -Xmx1536m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:InitiatingHeapOccupancyPercent=35 \
  -XX:G1NewCollectionThreads=4 \
  -XX:G1ReservePercent=10 \
  -XX:G1HeapWastePercent=5 \
  -XX:G1MixedGCCountTarget=8 \
  -XX:G1MixedGCLiveThresholdPercent=85 \
  -XX:+ParallelRefProcEnabled \
  -XX:+AlwaysPreTouch \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:G1SummarizeRSetStatsPeriod=86400 \
  -Djava.awt.headless=true \
  -Dspring.jmx.enabled=true \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9010 \
  -Dcom.sun.management.jmxremote.local.only=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false"

# For 4GB+ RAM machine (recommended for production):
export JAVA_OPTS_PROD="\
  -server \
  -Xms2048m \
  -Xmx3072m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=150 \
  -XX:InitiatingHeapOccupancyPercent=30 \
  -XX:G1NewCollectionThreads=6 \
  -XX:G1ReservePercent=15 \
  -XX:G1HeapWastePercent=5 \
  -XX:G1MixedGCCountTarget=10 \
  -XX:G1MixedGCLiveThresholdPercent=85 \
  -XX:+ParallelRefProcEnabled \
  -XX:+AlwaysPreTouch \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:G1SummarizeRSetStatsPeriod=86400 \
  -Djava.awt.headless=true \
  -Dspring.jmx.enabled=true \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9010 \
  -Dcom.sun.management.jmxremote.local.only=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false"

# ============================================
# EXPLANATION OF PARAMETERS
# ============================================

: '
-server                           : Use server VM (optimized for throughput)
-Xms1024m                        : Initial heap size (4GB for prod)
-Xmx1536m                        : Maximum heap size (6GB for prod)

G1GC (Garbage First Garbage Collector):
-XX:+UseG1GC                     : Use G1 GC (low pause time, predictable)
-XX:MaxGCPauseMillis=200         : Target max GC pause time (200ms acceptable for 10-15 users)
-XX:InitiatingHeapOccupancyPercent=35  : Start concurrent marking at 35% heap usage
-XX:G1NewCollectionThreads=4     : Parallel young GC threads (= CPU cores / 2)
-XX:G1ReservePercent=10          : Reserve 10% for mixed collections
-XX:G1HeapWastePercent=5         : Waste percentage before mixed collection
-XX:G1MixedGCCountTarget=8       : Run 8 mixed GCs per young collection cycle
-XX:G1MixedGCLiveThresholdPercent=85  : Only include regions with <85% live objects

-XX:+ParallelRefProcEnabled      : Use parallel processing for weak references
-XX:+AlwaysPreTouch              : Pre-touch memory pages (reduces major GC pauses)

JMX Monitoring:
-Dcom.sun.management.jmxremote   : Enable JMX remote monitoring
-Dcom.sun.management.jmxremote.port=9010  : JMX port (accessible for monitoring tools)
'

# ============================================
# STARTUP COMMAND
# ============================================

echo "Starting Gas Inventory System with Optimized JVM Settings..."

# Check environment
if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME not set"
    exit 1
fi

JAVA_VERSION=$($JAVA_HOME/bin/java -version 2>&1 | grep -oP 'version "\K[^"]*')
echo "Java Version: $JAVA_VERSION"

# Determine RAM and use appropriate settings
TOTAL_RAM=$(grep MemTotal /proc/meminfo | awk '{print int($2/1024/1024)}')
echo "Total RAM: ${TOTAL_RAM}GB"

if [ "$TOTAL_RAM" -ge 4 ]; then
    echo "Using PRODUCTION JVM settings (3GB heap)"
    JAVA_OPTS=$JAVA_OPTS_PROD
else
    echo "Using STANDARD JVM settings (1.5GB heap)"
fi

# ============================================
# RUN APPLICATION
# ============================================

cd "$(dirname "$0")/backend"

# Compile (if needed)
echo "Building application..."
mvn clean package -DskipTests

# Run with optimized JVM
echo "Starting application with optimized JVM..."
$JAVA_HOME/bin/java \
  $JAVA_OPTS \
  -Dspring.profiles.active=prod \
  -Dspring.application.name=gas-agency-system \
  -jar target/gas-agency-system-1.0.0.jar

# ============================================
# MONITORING
# ============================================

# Monitor JVM metrics:
# jconsole (GUI) - $JAVA_HOME/bin/jconsole
# jstat command line:
#   jstat -gc -h20 <pid> 1000  (GC stats every 1 second)
#   jstat -gccause <pid>        (GC cause analysis)
# 
# JMX clients:
#   localhost:9010 (no authentication needed in this config)
#   Tools: JConsole, VisualVM, JProfiler, YourKit
