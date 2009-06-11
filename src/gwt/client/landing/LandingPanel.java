//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.ui.MsoyUI;
import client.ui.RegisterPanel;

/**
 * Our main landing page.
 */
public class LandingPanel extends SmartTable
{
    public LandingPanel ()
    {
        super("landing", 0, 20);

        // create a UI explaining briefly what Whirled is
        FlowPanel explain = MsoyUI.createFlowPanel("Explain");
        explain.add(MsoyUI.createLabel(_msgs.landingIntro(), "Title"));
        explain.add(MsoyUI.createLabel(_msgs.landingIntroSub(), "Subtitle"));
        final SimplePanel video = new SimplePanel();
        video.setStyleName("Video");
        video.setWidget(MsoyUI.createActionImage("/images/landing/play_screen.png",
                                                 _msgs.landingClickToStart(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                video.setWidget(WidgetUtil.createFlashContainer(
                    "preview", "/images/landing/landing_movie.swf", 208, 154, null));
            }
        }));
        explain.add(video);

        // wrap all that up in two columns with header and background and whatnot
        setWidget(0, 0, explain);
        getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_LEFT);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        setWidget(0, 1, new RegisterPanel(true));
        getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        setWidget(1, 0, LandingCopyright.addFinePrint(new FlowPanel()), 2, null);
        getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
