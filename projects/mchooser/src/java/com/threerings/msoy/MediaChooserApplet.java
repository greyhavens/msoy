//
// $Id$

package com.threerings.msoy;

import java.applet.Applet;
import java.util.Observer;

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
        try {
            Class mcfc = Class.forName("com.threerings.msoy.mchooser.MediaChooserBridge");
            // we use this Observable hack because we need an interface known to the 1.4 VM that
            // allows us to pass an argument; we can't use reflection because we're running in
            // unsigned code even though we're calling into signed code
            Observer obs = (Observer)mcfc.newInstance();
            obs.update(null, this);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
