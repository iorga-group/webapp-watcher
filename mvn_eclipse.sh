#!/bin/bash
# Copyright (C) 2013 Iorga Group 
#  
# This program is free software: you can redistribute it and/or modify 
# it under the terms of the GNU Lesser General Public License as published by 
# the Free Software Foundation, either version 3 of the License, or 
# (at your option) any later version. 
#  
# This program is distributed in the hope that it will be useful, 
# but WITHOUT ANY WARRANTY; without even the implied warranty of 
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
# GNU Lesser General Public License for more details. 
#  
# You should have received a copy of the GNU Lesser General Public License 
# along with this program.  If not, see [http://www.gnu.org/licenses/].
set -x

# Create trap function which will stops the program if there is a problem with sub-calls
trap catch_error ERR;
function catch_error {
	echo "Problem occured, stopping. (last return code : $?)"
	exit 2
}

trap catch_int INT;
function catch_int {
	echo "Stopped with INT signal."
	exit 3
}

cd webappwatcher-parent
mvn install

cd ../iraj-bom
mvn install

cd ../waw-analyzer-parent
mvn clean
mvn eclipse:clean
mvn eclipse:eclipse "-Dwaw-analyzer.tag=`git describe --tags`"

echo "End of script"
read PAUSE
