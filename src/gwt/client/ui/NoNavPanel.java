//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import client.ui.MsoyUI;

/**
 * A panel that displays its contents wrapped in a blue-ish tongue with a short Whirled header
 * above it. Meant for headerless pages.
 */
public class NoNavPanel extends FlowPanel
{
    public static Widget makeBlue (Widget content)
    {
        FlowPanel panel = MsoyUI.createFlowPanel("nonav");
        panel.addStyleName("nonavBlue");
        panel.add(MsoyUI.createImage("/images/ui/nonav/header_blue.jpg", null));
        panel.add(MsoyUI.createSimplePanel(content, "Content"));
        panel.add(MsoyUI.createImage("/images/ui/nonav/footer_blue.png", null));
        return panel;
    }

    public static Widget makeWhite (Widget content)
    {
        FlowPanel panel = MsoyUI.createFlowPanel("nonav");
        panel.addStyleName("nonavWhite");
        panel.add(MsoyUI.createImage("/images/ui/nonav/header_white.jpg", null));
        panel.add(MsoyUI.createSimplePanel(content, "Content"));
        panel.add(MsoyUI.createImage("/images/ui/nonav/footer_white.png", null));
        return panel;
    }
}
