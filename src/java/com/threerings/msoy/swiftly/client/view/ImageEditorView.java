//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.util.Arrays;

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

    // from ImageEditor
    public void loadDocument (SwiftlyImageDocument doc)
    {
        // only refresh the image if the image data has changed in the new document
        if (!Arrays.equals(doc.getImage(), _document.getImage())) {
            displayImage();
        }

        // update the document reference to point at the new document
        _document = doc;
     }

    private void displayImage ()
    {
        _label.setIcon(new ImageIcon(_document.getImage()));
    }

    private SwiftlyImageDocument _document;
    private JLabel _label;
}
