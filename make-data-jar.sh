#!/bin/tcsh -x
# Build a data jar
set JARS=`pwd`/jars
set RESOURCES=`pwd`/resources

rm -rf tmp
mkdir tmp
mkdir tmp/resources
mkdir tmp/resources/files

cp -rX $RESOURCES/images tmp/resources
cp -rX $RESOURCES/css tmp/resources
cp -rX $RESOURCES/icons tmp/resources
cp -rX $RESOURCES/files tmp/resources

cd tmp
jar -cvf $JARS/data.jar resources/*
rm -rf tmp
