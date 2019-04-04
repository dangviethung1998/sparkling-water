#!/usr/bin/env bash

# Current dir
TOPDIR=$(cd "$(dirname "$0")/.."; pwd)

source "$TOPDIR/bin/sparkling-env.sh"
# Java check
checkJava
# Verify there is Spark installation
checkSparkHome
# Verify if correct Spark version is used
checkSparkVersion
# Check sparkling water assembly Jar exists
checkFatJarExists
DRIVER_CLASS=water.SparklingWaterDriver

DRIVER_MEMORY=${DRIVER_MEMORY:-$DEFAULT_DRIVER_MEMORY}
MASTER=${MASTER:-"$DEFAULT_MASTER"}
VERBOSE=--verbose
VERBOSE=

# Show banner
banner 

spark-submit "$@" $VERBOSE --driver-memory "$DRIVER_MEMORY" --master "$MASTER" --class "$DRIVER_CLASS" "$FAT_JAR_FILE"

