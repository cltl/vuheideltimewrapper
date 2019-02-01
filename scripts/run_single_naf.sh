#!/bin/bash

f="$1"

if [ -z $1 ]; then
	echo "Please specify a file. Exiting now..."
	exit
fi

base=$(basename $f)
echo "File to process: $base"
java -Xmx2000m -cp ../target/vu-heideltime-wrapper-1.0-jar-with-dependencies.jar  vu.cltl.vuheideltimewrapper.CLI --naf-file $1 --mapping ../lib/alpino-to-treetagger.csv --config ../conf/config.props
