//
// $Id$

package com.threerings.msoy.mchooser.modes;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

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
        throws IOException
    {
        String name = imageSource.getFile();
        name = name.substring(name.lastIndexOf("/")+1);
        init(name, IOUtils.toByteArray(imageSource.openStream()));
    }

    // from interface MediaChooser.Mode
    public void activate (MediaChooser chooser)
    {
        _chooser = chooser;
        _chooser.setSidebar(_tip);
        _chooser.setMain(_preview);
    }

    // from interface MediaChooser.Mode
    public void deactivated ()
    {
    }

    protected void init (final String name, final byte[] media)
        throws IOException
    {
        _image = ImageIO.read(new MemoryCacheImageInputStream(new ByteArrayInputStream(media)));
        if (_image == null) {
            throw new IOException("Unable to decode " + name);
        }

        _tip = new JLabel("Preview");

        // TODO: allow scrolling around, etc.
        _preview = new JPanel(new BorderLayout(5, 5));
        if (media != null) {
            _preview.add(new JLabel(new ImageIcon(_image)), BorderLayout.CENTER);
        } else {
            _preview.add(new JLabel("Error loading media."), BorderLayout.CENTER);
        }

        JPanel uprow = new JPanel(new HGroupLayout(HGroupLayout.NONE, HGroupLayout.RIGHT));
        JButton upload = new JButton("Upload");
        upload.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _chooser.pushMode(new UploadMediaMode(name, media));
            }
        });
        uprow.add(upload);
        _preview.add(uprow, BorderLayout.SOUTH);
    }

    protected MediaChooser _chooser;
    protected BufferedImage _image;

    protected JLabel _tip;
    protected JPanel _preview;
}
