#!/bin/bash

f="$1"
in="$2"
out="$3"

if [ -z $1 ]; then
	echo "Please specify a folder. Exiting now..."
	exit
fi

base=$(basename $f)
echo "Folder to process: $base"
java -Xmx2000m -cp ../target/vu-heideltime-wrapper-1.0-jar-with-dependencies.jar  vu.cltl.vuheideltimewrapper.CLI --naf-folder $1 --extension-in $in --extension-out $out --mapping ../lib/alpino-to-treetagger.csv --config ../conf/config.props
