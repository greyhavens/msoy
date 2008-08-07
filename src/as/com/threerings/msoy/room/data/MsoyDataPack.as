//
// $Id$

package com.threerings.msoy.room.data {

import flash.utils.ByteArray;

import nochump.util.zip.ZipEntry;
import nochump.util.zip.ZipFile;
import nochump.util.zip.ZipOutput;

import com.whirled.DataPack;

/**
 * A DataPack with a few extra functions for use inside whirled.
 * Note: This is different from an EditableDataPack, which is currently only in Java
 * and lives in com.whirled.remix.data.
 */
public class MsoyDataPack extends DataPack
{
    public function MsoyDataPack (urlOrByteArray :*)
    {
        super(urlOrByteArray);
    }

    /**
     * Get the primary content.
     */
    public function getContent () :ByteArray
    {
        return getFile(CONTENT_DATANAME);
    }

    override protected function validateName (name :String) :void
    {
        switch (name) {
        case CONTENT_DATANAME: // this name is OK here
            break;

        default:
            super.validateName(name);
            break;
        }
    }
}
}
