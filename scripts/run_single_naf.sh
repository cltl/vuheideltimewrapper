#!/bin/bash

f="$1"

if [ -z $1 ]; then
	echo "Please specify a file. Exiting now..."
	exit
fi

base=$(basename $f)
echo "File to process: $base"
cat $f | java -Xmx2000m -cp ../target/vu-heideltime-wrapper-1.0-jar-with-dependencies.jar;../lib/de.unihd.dbs.heideltime.standalone.jar  vu.cltl.vuheideltimewrapper.CLI --mapping ../lib/alpino-to-treetagger.csv --config ../conf/config.props

