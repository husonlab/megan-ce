#!/bin/bash
#
# daa-meganizer Copyright (C) 2022 Daniel H. Huson
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

# Runs the DAA-Meganizer command-line program

options=$*
if [ $# == 0 ]
then
	options="-h"
fi	

bin_dir=$(dirname "$0")       # may be relative path
bin_dir=$(cd "$bin_dir" && pwd)    # ensure absolute path

jars_dir="$bin_dir/../jars"
jars2_dir="$bin_dir/../jars2"

jre_dir=${installer:sys.preferredJre}

java=$jre_dir/bin/java
vmOptions=$(grep "^-" $bin_dir/../MEGAN.vmoptions)

modulepath="$jars_dir:$jars2_dir"

java_flags="-server -Duser.language=en -Duser.region=US $vmOptions"

$java $java_flags --module-path=$modulepath --add-modules=megan megan.tools.DAAMeganizer  $options
