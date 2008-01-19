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
import com.samskivert.swing.VGroupLayout;
import org.apache.commons.io.IOUtils;

import com.threerings.msoy.mchooser.MediaChooser;
import com.threerings.msoy.mchooser.editors.ImageEditor;
import com.threerings.msoy.mchooser.editors.ImageEditorPalette;

import static com.threerings.msoy.mchooser.MediaChooser.log;

/**
 * Provides an interface for cropping, scaling and otherwise editing an image prior to uploading it
 * to Whirled.
 */
public class EditImageMode
    implements MediaChooser.Mode
{
    public EditImageMode (URL imageSource)
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
        _chooser.setSidebar(_palette);
        _chooser.setMain(_contents);
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

        _contents = new JPanel(new BorderLayout(5, 5));
        _contents.add(_editor = new ImageEditor(), BorderLayout.CENTER);
        _palette = new ImageEditorPalette(_editor);
        _editor.getModel().setImage(_image);

        JPanel uprow = new JPanel(new HGroupLayout(HGroupLayout.NONE, HGroupLayout.RIGHT));
        JButton upload = new JButton("Upload");
        upload.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                try {
                    _chooser.pushMode(new UploadMediaMode(name, _editor.getModel().getImageBytes()));
                } catch (IOException ioe) {
                    // TODO: display error
                    ioe.printStackTrace(System.err);
                }
            }
        });
        uprow.add(upload);
        _contents.add(uprow, BorderLayout.SOUTH);
    }

    protected MediaChooser _chooser;
    protected BufferedImage _image;

    protected ImageEditorPalette _palette;
    protected JPanel _contents;
    protected ImageEditor _editor;
}
