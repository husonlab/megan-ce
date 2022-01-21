#!/bin/tcsh -x
#
# make-data-jar.sh Copyright (C) 2022 Daniel H. Huson
#
# (Some files contain contributions from other authors, who are then mentioned separately.)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

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
cd ..
rm -rf tmp
