//
// $Id$

package com.threerings.msoy.swiftly.client;        

import java.util.List;

import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;

public interface SwiftlyDocumentEditor
{
    /**
      * Requests the supplied SwiftlyTextDocument be opened in this editor at the supplied
      * row and column.
      */
    public void editTextDocument (SwiftlyTextDocument document, int row, int column);

    public List<FileTypes> getCreateableFileTypes ();

    /** Maps a human friendly name to a mime type. */
    public static class FileTypes
    {
        public FileTypes (String displayName, String mimeType)
        {
            this.displayName = displayName;
            this.mimeType = mimeType;
        }

        public String toString()
        {
            return displayName;
        }

        public String displayName;
        public String mimeType;
    }
}
