//
// $Id$

package com.threerings.msoy.mchooser.modes;

import java.io.IOException;
import java.net.URL;

import javax.swing.JLabel;

import com.threerings.msoy.mchooser.Config;
import com.threerings.msoy.mchooser.MediaChooser;
import com.threerings.msoy.mchooser.MediaSource;
import com.threerings.msoy.mchooser.SourcePanel;
import com.threerings.msoy.mchooser.sources.LocalFileSource;

import static com.threerings.msoy.mchooser.MediaChooser.log;

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
        try {
            _chooser.pushMode(new PreviewImageMode(media));
        } catch (IOException ioe) {
            log.info("Unable to preview media [media=" + media + ", error=" + ioe + "].");
            _chooser.pushMode(new UploadMediaMode(media));
        }
    }

    protected MediaChooser _chooser;
    protected SourcePanel _sources;
}
