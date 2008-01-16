//
// $Id$

package com.threerings.msoy.mchooser.modes;

import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.threerings.msoy.mchooser.MediaChooser;

/**
 * Displays a preview of an image and allows it to be communicated back to Whirled if the user is
 * happy with it.
 */
public class PreviewImageMode
    implements MediaChooser.Mode
{
    public PreviewImageMode (URL imageSource)
    {
        // TODO: allow scrolling around, etc.
        _tip = new JLabel("Preview");
        _preview = new JLabel(new ImageIcon(imageSource));
    }

    // from interface MediaChooser.Mode
    public void activate (MediaChooser chooser)
    {
        chooser.setSidebar(_tip);
        chooser.setMain(_preview);
    }

    protected JLabel _tip;
    protected JLabel _preview;
}
