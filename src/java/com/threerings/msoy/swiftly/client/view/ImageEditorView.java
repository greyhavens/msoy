//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.threerings.msoy.swiftly.data.SwiftlyImageDocument;

/**
 * Implementation of ImageEditor.
 */
public class ImageEditorView extends JPanel
    implements ImageEditor
{
    public ImageEditorView (SwiftlyImageDocument document)
    {
        _document = document;

        add(_label = new JLabel());

        displayImage();
    }

    // from DocumentEditor
    public SwiftlyImageDocument getSwiftlyDocument ()
    {
        return _document;
    }

    // from DocumentEditor
    public void loadDocument (SwiftlyImageDocument doc)
    {
        SwiftlyImageDocument currentDoc = _document;
        _document = doc;

        // only refresh the image if the image data has changed in the new document
        if (!doc.contentsEqual(currentDoc)) {
            displayImage();
        }
    }

    private void displayImage ()
    {
        _label.setIcon(new ImageIcon(_document.getImage()));
    }

    private SwiftlyImageDocument _document;
    private JLabel _label;
}
