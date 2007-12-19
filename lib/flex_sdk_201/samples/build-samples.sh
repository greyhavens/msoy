#!/bin/sh

buildFiles=`find . -name 'build.sh' -print`

for buildFile in ${buildFiles}; do
	echo "processing $buildFile"
	(cd `dirname ${buildFile}`;
	exec ./build.sh)
done

