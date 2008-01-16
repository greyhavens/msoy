//
// $Id$

package com.threerings.msoy;

import java.applet.Applet;
import javax.swing.JFrame;

import java.awt.Button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Displays a "Change" button which pops up the media chooser.
 */
public class MediaChooserApplet extends Applet
{
    // @Override // from Applet
    public void init ()
    {
        super.init();

        Button change = new Button("Change");
        change.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                openMediaChooser();
            }
        });
        add(change);
    }

    protected void openMediaChooser ()
    {
        System.err.println("Doin' my biz!");
        try {
            Class mcfc = Class.forName("com.threerings.msoy.mchooser.MediaChooserFrame");
            JFrame frame = (JFrame)mcfc.newInstance();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
