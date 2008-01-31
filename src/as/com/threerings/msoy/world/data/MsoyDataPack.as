//
// $Id$

package com.threerings.msoy.world.data {

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

//    /**
//     * Completely remove the specified file.
//     */
//    public function removeFile (name :String) :Boolean
//    {
//        var value :String  = getFileName(name);
//        if (value == null) {
//            return false;
//        }
//
//        // remove the actual file from the zip
//        for (var ii :int = _zip.getFileCount() - 1; ii >= 0; ii--) {
//            var file :FZipFile = _zip.getFileAt(ii);
//            if (file.filename == value) {
//                _zip.removeFileAt(ii);
//                break;
//            }
//        }
//
//        // delete the record for the file from the metadata
//        delete _metadata..file.(@name == name)[0];
//
//        // and we-write the metadata inside the zip
//        var dataFile :FZipFile = _zip.getFileByName(METADATA_FILENAME);
//        dataFile.setContentAsString(String(_metadata));
//
//        return true;
//    }

    /**
     * Turn this datapack into a ByteArray that may be passed to another
     * DataPack instance.
     */
    public function toByteArray () :ByteArray
    {
        var outZip :ZipOutput = new ZipOutput();
        for each (var entry :ZipEntry in _zip.entries) {
            outZip.putNextEntry(entry);
            outZip.write(_zip.getInput(entry));
            outZip.closeEntry();
        }
        outZip.finish();
        return outZip.byteArray;
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
