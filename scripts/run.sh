#!/bin/bash
#
# NAME: run.sh
# DATE: 15/02/2022
#
# This script runs the vu-heideltime wrapper on input folders or files.
# The output path may be a folder or a file, depending on the input:
# - input folder -> output folder: processed files in the output folder have the same basename as the input files
# - input file -> output folder: the processed file in the output folder has the same name as the input file
# - input file -> output file: the processed file has the specified output name
#
# -------------------------------------------------------------------------------------------------------
in=$1  # input folder/file
out=$2  # output folder/file

if [ $# -ne 2 ]; then
  echo "Usage: sh run.sh INPUT_PATH OUTPUT_PATH"
  exit 1
fi

workdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)

java -jar ${workdir}/target/vu-heideltime-wrapper-1.1.jar --input $in --output $out
