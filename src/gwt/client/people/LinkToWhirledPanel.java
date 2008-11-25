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

        add(MsoyUI.createHTML(_msgs.linkToWhirledIntro(), "IntroText"));

        // TODO: maybe these should go on s3?
        String imageBase = "/images/people/links/";
        String[] imageFiles = {"Whirled_ad_play.jpg", "whirledBG.jpg",
            "whirled_logo_NEW_justlogo.png"};
        for (String imageFile : imageFiles) {
            add(new LogoWidget(imageBase + imageFile));
        }
    }

    /**
     * Simple widget with an image and a text box with the html code below.
     */
    public static class LogoWidget extends SmartTable
    {
        /**
         * Creates a new logo widget.
         */
        public LogoWidget (String imagePath)
        {
            addStyleName("LogoTable");
            setWidget(0, 0, new Image(imagePath));
            TextBox text = new TextBox();
            text.setText("<a href=\"" + SharePanel.getAffiliateLandingUrl() + "\"><img src=\"" +
                DeploymentConfig.serverURL + imagePath.substring(1) + "\"></a>");
            text.setMaxLength(400);
            text.setWidth("100%");
            MsoyUI.selectAllOnFocus(text);
            setWidget(1, 0, text);
        }
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
