//
// $Id$

package com.threerings.msoy.mchooser.modes;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.URL;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.HGroupLayout;
import org.apache.commons.io.IOUtils;

import com.threerings.msoy.mchooser.MediaChooser;

import static com.threerings.msoy.mchooser.MediaChooser.log;

/**
 * Displays a preview of an image and allows it to be communicated back to Whirled if the user is
 * happy with it.
 */
public class PreviewImageMode
    implements MediaChooser.Mode
{
    public PreviewImageMode (URL imageSource)
    {
        try {
            init(IOUtils.toByteArray(imageSource.openStream()));
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to read image media [url=" + imageSource + "].", e);
        }
    }

    // from interface MediaChooser.Mode
    public void activate (MediaChooser chooser)
    {
        _chooser = chooser;
        _chooser.setSidebar(_tip);
        _chooser.setMain(_preview);
    }

    protected void init (byte[] media)
    {
        _tip = new JLabel("Preview");

        // TODO: allow scrolling around, etc.
        _preview = new JPanel(new BorderLayout(5, 5));
        if (media != null) {
            _preview.add(new JLabel(new ImageIcon(media)), BorderLayout.CENTER);
        } else {
            _preview.add(new JLabel("Error loading media."), BorderLayout.CENTER);
        }

        JPanel uprow = new JPanel(new HGroupLayout(HGroupLayout.NONE, HGroupLayout.RIGHT));
        JButton upload = new JButton("Upload");
        upload.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                // TODO: _chooser.pushMode(new UploadMediaMode(_media));
            }
        });
        uprow.add(upload);
        _preview.add(uprow, BorderLayout.SOUTH);
    }

    protected byte[] _media;
    protected MediaChooser _chooser;
    protected JLabel _tip;
    protected JPanel _preview;
}
