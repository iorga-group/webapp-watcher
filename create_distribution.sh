#!/bin/bash
set -x

TAG=$1

TMP_DIR=/dev/shm/webappwatcher-distribution

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

rm -rf $TMP_DIR
git clone ./ $TMP_DIR -b master

cd $TMP_DIR

if [ -n "$TAG" ]
then
	git checkout $TAG
else
	TAG=`git describe --tags`
fi

#sed -i "s/\\\$Id.*\\\$/`git describe --tags`/" isi/pom.xml isi-parent/pom.xml seam-blank-parent/pom.xml

# Suppression des références "SNAPSHOT"
find . -name "pom.xml" -exec sed -i "s/-SNAPSHOT//g" {} \;

NEW_RELEASE_DIR_NAME=webappwatcher-distribution-${TAG#webappwatcher-}
NEW_RELEASE_DIR=/dev/shm/$NEW_RELEASE_DIR_NAME

rm -rf $NEW_RELEASE_DIR

# Copie des sources
mkdir -p $NEW_RELEASE_DIR/src
cp -r * .git* $NEW_RELEASE_DIR/src/

# Build de la distribution
cd webappwatcher-parent
mvn package
# Copie des fichiers générés
cd ..
cp webappwatcher-web/target/webappwatcher-web-*.war $NEW_RELEASE_DIR/
mkdir tmp
cd tmp
tar xvzf ../webappwatcher/target/*-distribution.tar.gz
cp -R webappwatcher-*/* $NEW_RELEASE_DIR/
cd ..

rm -f $NEW_RELEASE_DIR_NAME.7z

cd $NEW_RELEASE_DIR
7z a ../$NEW_RELEASE_DIR_NAME.7z *


echo "End of script"
read PAUSE
