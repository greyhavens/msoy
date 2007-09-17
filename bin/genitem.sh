#!/bin/sh
#
# $Id$
#
# Generates the bevy of classes needed to add a new item type to Whirled.

ROOT=`dirname $0`/..
ROOT=`cd $ROOT ; pwd`

if [ -z "$1" ]; then
    echo "Usage: $0 Thing"
    echo "(generates ThingRecord.java ThingCloneRecord.java ...)"
    exit 255;
fi

ITEM="$1"
UPITEM=`echo $ITEM | tr '[:lower:]' '[:upper:]'`
shift

ITEM_DIR=$ROOT/src/java/com/threerings/msoy/item/data/all
ASITEM_DIR=$ROOT/src/as/com/threerings/msoy/item/data/all
PERS_DIR=$ROOT/src/java/com/threerings/msoy/item/server/persist
if [ ! -f $PERS_DIR/TemplateRecord.tmpl ]; then
    echo "Can't find TemplateRecord.tmpl in $PERS_DIR...";
    exit 255;
fi

for FILE in $ITEM_DIR/*.tmpl $PERS_DIR/*.tmpl ; do
    SOURCE=`echo $FILE | sed "s:Template:$ITEM:g" | sed "s:.tmpl:.java:"`
    if [ -f $SOURCE ]; then
        echo "Not generating $SOURCE. It already exists."
    else
        echo "Generating $SOURCE..."
        cat $FILE | sed "s:Template:$ITEM:g" | sed "s:TEMPLATE:$UPITEM:g" > $SOURCE
    fi
done

for FILE in $ASITEM_DIR/*.tmpl ; do
    SOURCE=`echo $FILE | sed "s:Template:$ITEM:g" | sed "s:.tmpl:.as:"`
    if [ -f $SOURCE ]; then
        echo "Not generating $SOURCE. It already exists."
    else
        echo "Generating $SOURCE..."
        cat $FILE | sed "s:Template:$ITEM:g" | sed "s:TEMPLATE:$UPITEM:g" > $SOURCE
    fi
done

echo ""
echo "Done! Be sure to update Item.java and Item.as to add the $UPITEM constant."
echo "Also be sure to add a GWT ${ITEM}Editor to src/gwt/client/editem/ItemEditor.java"
