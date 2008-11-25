//
// $Id$

package client.landing;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import client.ui.MsoyUI;

/**
 * Styled box for displaying full-width content on landing pages.
 */
public class WideContentBox extends FlowPanel
{
    public WideContentBox (Widget content)
    {
        this(null, content, false);
    }

    public WideContentBox (String title, String content, boolean altTitle)
    {
        this(title, MsoyUI.createHTML(content, null), altTitle);
    }

    public WideContentBox (String title, Widget content, boolean altTitle)
    {
        setStyleName("wideContentBox");
        add(MsoyUI.createFlowPanel("BoxTop"));
        FlowPanel boxContent = MsoyUI.createFlowPanel("BoxContent");
        if (title != null) {
            boxContent.add(MsoyUI.createLabel(title, altTitle ? "TitleAlt" : "Title"));
        }
        if (content != null) {
            boxContent.add(content);
        }
        add(boxContent);
        add(MsoyUI.createFlowPanel("BoxBottom"));
    }
}
