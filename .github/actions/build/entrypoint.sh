#!/bin/sh

echo "-----------------------------------------------------"
echo "JARFILE : $INPUT_JARFILE"
echo "ARTIFACT: $INPUT_ARTIFACT"
echo "-----------------------------------------------------"

./mvnw clean package && cd target && tar cvfz ${INPUT_ARTIFACT} ${INPUT_JARFILE}
