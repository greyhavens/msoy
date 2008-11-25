//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.DeploymentConfig;

import client.ui.MsoyUI;
import client.ui.RoundBox;

/**
 * Simple panel showing a bunch of whirled logos and banners with copy-n-paste HTML code to allow
 * users to link to the affiliate landing page.
 */
public class LinkToWhirledPanel extends VerticalPanel
{
    /**
     * Creates a new panel.
     */
    public LinkToWhirledPanel ()
    {
        setSpacing(10);
        setStyleName("linkToWhirled");

        add(MsoyUI.createHTML(_msgs.linkToWhirledIntro(), null));

        // TODO: maybe these should go on s3?
        String imageBase = "/images/people/links/";
        String[] imageFiles = {"Whirled_ad_play.jpg", "whirledbanner_02_play.jpg",
            "whirled_banner.png", "whirledBG.jpg", "Whirled_flatlogo.png",
            "whirled_logo_NEW_justlogo.png"};
        for (String imageFile : imageFiles) {
            add(new LogoWidget(imageBase + imageFile));
        }
    }

    /**
     * Simple widget with an image and a text box with the html code below.
     */
    public static class LogoWidget extends RoundBox
    {
        /**
         * Creates a new logo widget.
         */
        public LogoWidget (String imagePath)
        {
            super(RoundBox.DARK_BLUE);
            addStyleName("LogoBox");
            SmartTable table = new SmartTable();
            table.setWidget(0, 0, new Image(imagePath), 1, null);
            TextBox text = new TextBox();
            text.setText("<a href=\"" + SharePanel.getAffiliateLandingUrl() + "\"><img src=\"" +
                DeploymentConfig.serverURL + imagePath.substring(1) + "\"></a>");
            text.setMaxLength(400);
            text.setWidth("100%");
            table.setWidget(1, 0, text);
            add(table);
        }
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
