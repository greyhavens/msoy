#!/bin/sh
#
# Works around a bug in GWT's I18NSync class; when they fix it, this can go away

for FILE in `find src/gwt -name '*Messages.java'`; do
    if [ "$FILE" != "src/gwt/client/shell/DynamicMessages.java" -a \
        "$FILE" != "src/gwt/client/shell/ServerMessages.java" ]; then
        echo Fixing $FILE
        grep -v gwt.key $FILE > $FILE.new
        mv $FILE.new $FILE
    fi
done
