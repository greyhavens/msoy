#!/bin/sh
#
# Works around a bug in GWT's I18NSync class; when they fix it, this can go away

for FILE in `find src/gwt -name '*Messages.java'`; do
    grep -v gwt.key $FILE > $FILE.new
    mv $FILE.new $FILE
done
