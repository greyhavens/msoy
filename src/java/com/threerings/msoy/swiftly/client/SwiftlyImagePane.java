//
// $Id$

package com.threerings.msoy.swiftly.client;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.util.Arrays;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyImageDocument;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class SwiftlyImagePane extends JPanel
    implements SetListener
{
    public SwiftlyImagePane (SwiftlyContext ctx, SwiftlyImageDocument document)
    {
        _ctx = ctx;
        _document = document;

        add(_label = new JLabel());

        displayImage();
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        // nada
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            SwiftlyDocument element = (SwiftlyDocument)event.getEntry();
            // check to see if the updated document is the one being displayed
            if (element.documentId == _document.documentId) {
                SwiftlyImageDocument newDoc = (SwiftlyImageDocument)element;
                SwiftlyImageDocument oldDoc = (SwiftlyImageDocument)event.getOldEntry();

                // update the document reference to point at the new document
                _document = newDoc;

                // only refresh the image if the image data has changed in the new document
                if (!Arrays.equals(newDoc.getImage(), oldDoc.getImage())) {
                    // display the new image contents
                    displayImage();
                }
            }
        }
    }
    
    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        // nada
    }

    protected void displayImage ()
    {
        _label.setIcon(new ImageIcon(_document.getImage()));
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyImageDocument _document;

    protected JLabel _label;
}
