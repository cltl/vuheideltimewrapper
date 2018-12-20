#!/bin/bash

extensions=(naf)

for ext in "${extensions[@]}"
do
	echo "Look for files with extension: $ext"
	if [ -e ../nafs_to_process/*.$ext ]; then
		echo "Files with extension $ext exist. Now processing them ..."
		for f in ../nafs_to_process/*.$ext ;
		do
			base=$(basename $f)
			echo "File: $base"
			cat $f | java -Xmx2000m -cp ../target/vu-heideltime-wrapper-1.0-jar-with-dependencies.jar vu.cltl.vuheideltimewrapper.CLI --mapping ${dirToFile}/alpino-to-treetagger.csv --config ${dirWithFile}/config.props
		done
	else
		echo "No files with extension $ext exist in the directory ../nafs_to_process! "
	fi
done
