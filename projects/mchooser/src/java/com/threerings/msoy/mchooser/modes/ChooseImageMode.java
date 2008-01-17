//
// $Id$

package com.threerings.msoy.mchooser.modes;

import java.net.URL;

import javax.swing.JLabel;

import com.threerings.msoy.mchooser.Config;
import com.threerings.msoy.mchooser.MediaChooser;
import com.threerings.msoy.mchooser.MediaSource;
import com.threerings.msoy.mchooser.SourcePanel;
import com.threerings.msoy.mchooser.sources.LocalFileSource;

/**
 * Displays the image choosing interface.
 */
public class ChooseImageMode
    implements MediaChooser.Mode, MediaSource.ResultReceiver
{
    public ChooseImageMode ()
    {
        _sources = new SourcePanel(this);
        _sources.addSource(new LocalFileSource(Config.IMAGE));
    }

    // from interface MediaChooser.Mode
    public void activate (MediaChooser chooser)
    {
        _chooser = chooser;
        _sources.activate(chooser);
    }

    // from interface MediaChooser.Mode
    public void deactivated ()
    {
    }

    // from interface MediaSource.ResultReceiver
    public void mediaSelected (URL media)
    {
        _chooser.pushMode(new PreviewImageMode(media));
    }

    protected MediaChooser _chooser;
    protected SourcePanel _sources;
}
