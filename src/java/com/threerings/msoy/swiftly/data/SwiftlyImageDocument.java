//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import javax.swing.ImageIcon;

import org.apache.commons.io.IOUtils;

import com.threerings.msoy.swiftly.client.controller.SwiftlyDocumentEditor;
import com.threerings.msoy.swiftly.client.view.PositionLocation;

/**
 * Represents an image file in a project and contains the data of the image.
 * Each document retains an unmodified copy of its initial data to allow for delta generation.
 */
public class SwiftlyImageDocument extends SwiftlyBinaryDocument
{
    public static class DocumentFactory
        implements SwiftlyDocument.DocumentFactory
    {
        public boolean handlesMimeType (String mimeType) {
            for (String type : _mimeTypes) {
                if (mimeType.startsWith(type)) {
                    return true;
                }
            }
            return false;
        }

        public ImageIcon createIcon ()
            throws IOException
        {
            URL path = getClass().getResource(DOCUMENT_ICON);
            if (path == null) {
                throw new IOException("Icon path for SwiftlyDocument not found: " + DOCUMENT_ICON);
            }
            return new ImageIcon(path);
        }

        public SwiftlyDocument createDocument (PathElement path, String encoding)
            throws IOException
        {
            return new SwiftlyImageDocument(path);
        }

        public SwiftlyDocument createDocument (InputStream data, PathElement path, String encoding)
            throws IOException
        {
            return new SwiftlyImageDocument(data, path);
        }

        /** Mime types supported by this document type. */
        private final String[] _mimeTypes = {"image/"};

        /** The path to the icon for an image document */
        private static final String DOCUMENT_ICON = "/rsrc/icons/swiftly/image_document.png";
    }

    /** Required for the dobj system. Do not use. */
    public SwiftlyImageDocument ()
    {
    }

    /** Instantiate a new, blank image document with the given path. */
    public SwiftlyImageDocument (PathElement path)
        throws IOException
    {
        this(null, path);
    }

    /** Instantiate an image document with the given data and path. */
    public SwiftlyImageDocument (InputStream data, PathElement path)
        throws IOException
    {
        super(data, path);
        // load up the image data into memory
        _image = IOUtils.toByteArray(getOriginalData());
    }

    /** Returns the image data as a byte array */
    public byte[] getImage ()
    {
        return _image;
    }

    /**
     * Returns true if the contents of this document are the same as the supplied document.
     */
    public boolean contentsEqual (SwiftlyImageDocument other)
    {
        return Arrays.equals(getImage(), other.getImage());
    }

    @Override // from SwiftlyBinaryDocument
    public void setData (InputStream data, String encoding)
        throws IOException
    {
        super.setData(data, encoding);
        // load up the image data into memory
        _image = IOUtils.toByteArray(getModifiedData());
    }

    @Override // from SwiftlyDocument
    public void loadInEditor (SwiftlyDocumentEditor editor, PositionLocation location)
    {
        editor.editImageDocument(this);
    }

    /** Image contents, inefficiently stored entirely in memory. */
    private byte[] _image;
}
