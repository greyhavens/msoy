//
// $Id$

package com.threerings.msoy.item.remix.client;

import java.awt.BorderLayout;

import javax.swing.JApplet;

public class RemixApplet extends JApplet
{
    @Override
    public void init ()
    {
        super.init();

        String mediaURL = getParameter("media");
        add(new RemixPanel(mediaURL, this), BorderLayout.CENTER);
    }

    @Override
    public String getAppletInfo ()
    {
        return "Whirled Remixer";
    }
}
