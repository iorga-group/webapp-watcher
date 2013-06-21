#!/bin/bash
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

cd waw-analyzer-parent
mvn clean
mvn eclipse:clean
mvn eclipse:eclipse

echo "End of script"
read PAUSE
