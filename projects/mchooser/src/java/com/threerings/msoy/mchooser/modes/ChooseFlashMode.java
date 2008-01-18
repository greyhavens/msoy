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
public class ChooseFlashMode
    implements MediaChooser.Mode, MediaSource.ResultReceiver
{
    public ChooseFlashMode ()
    {
        _sources = new SourcePanel(this);
        _sources.addSource(new LocalFileSource(Config.FLASH));
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
        // TODO: if it's an image, show the preview/editor
        _chooser.pushMode(new UploadMediaMode(media));
    }

    protected MediaChooser _chooser;
    protected SourcePanel _sources;
}
