#!/bin/sh
#
# A script to update the SDK which takes care of the few fiddly steps we currently have

MSOY_DIR=`dirname $0`
MSOY_DIR=`cd $MSOY_DIR/.. ; pwd`

WHIRLED_DIR=/export/whirled
if [ ! -d $WHIRLED_DIR ]; then
    echo "This script assumes Whirled is in $WHIRLED_DIR, which it does not appear to be."
    exit 255
fi

if [ -z "$1" ]; then
    echo "Usage: $0 new_version (e.g. '$0 0.9')"
    exit 255
fi

cd $WHIRLED_DIR
# ant sdk

cp $WHIRLED_DIR/dist/whirled_sdk.zip $MSOY_DIR/data/whirled_sdk_$1.zip
rsync -avrq $WHIRLED_DIR/dist/sdk/whirled/docs/* -type f $MSOY_DIR/pages/code/asdocs

echo ""
echo "Now delete the old SDK and commit the new one:"
echo ""
echo "  svn rm data/whirled_sdk_*.zip"
echo "  svn add data/whirled_sdk_$1.zip"
echo ""
echo "Add any new files in pages/code/asdocs:"
echo ""
echo "  svn status pages/code/asdocs | egrep '^\?' | awk '{ print \$2 }' | xargs svn add"
echo ""
echo "Make sure your handiwork is satisfactory:"
echo ""
echo "  svn status data pages/code/asdocs"
echo ""
echo "Then commit the new SDK and the modifications in pages/code/asdocs:"
echo ""
echo "  svn commit data pages/code/asdocs"
