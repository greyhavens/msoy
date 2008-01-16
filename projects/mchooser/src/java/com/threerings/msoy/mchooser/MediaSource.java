//
// $Id$

package com.threerings.msoy.mchooser;

import java.net.URL;

import javax.swing.JComponent;

/**
 * Represents a source of media.
 */
public interface MediaSource
{
    public interface ResultReceiver {
        public void mediaSelected (URL media);
    }

    public String getName ();

    public JComponent createChooser (ResultReceiver receiver);
}
