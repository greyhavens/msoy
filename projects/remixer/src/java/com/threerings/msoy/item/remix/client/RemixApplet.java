//
// $Id$

package com.threerings.msoy.item.remix.client;

import java.awt.BorderLayout;

import javax.swing.JApplet;

import com.samskivert.util.Interval;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;

public class RemixApplet extends JApplet
{
    @Override
    public void init ()
    {
        super.init();

        // ensure we setup the UI on the awt thread.
        RunQueue.AWT.postRunnable(new Runnable() {
            public void run () {
                String mediaURL = StringUtil.decode(getParameter("media"));
                add(new RemixPanel(mediaURL, RemixApplet.this), BorderLayout.CENTER);
            }
        });
    }

    @Override
    public String getAppletInfo ()
    {
        return "Whirled Remixer";
    }
}
