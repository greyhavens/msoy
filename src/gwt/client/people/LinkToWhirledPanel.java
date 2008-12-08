//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.DeploymentConfig;

import com.threerings.msoy.web.gwt.Pages;

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
        setSpacing(0);
        setStyleName("linkToWhirled");

        SmartTable top = new SmartTable(0, 0);
        top.setWidget(0, 0, MsoyUI.createLabel(_msgs.linkToWhirledTitle(), "Title"));
        top.setWidget(0, 1, MsoyUI.createHTML(
            _msgs.linkToWhirledIntro(), "Intro"), 1, "IntroCell");
        top.setWidth("100%");
        add(top);

        add(MsoyUI.createHTML(_msgs.linkToWhirledInstructions(), "Instructions"));

        add(_htmlCode = new TextArea());
        _htmlCode.setWidth("100%");
        _htmlCode.setVisibleLines(2);
        _htmlCode.setText(_msgs.linkToWhirledClickBelow());
        MsoyUI.selectAllOnFocus(_htmlCode);

        // TODO: hook these up to different landing areas
        String[] squareImageFiles = {
            "125ad.jpg",
            "125create.png",
            "125design.jpg",
            "125earn.png",
            "125findme.jpg",
            "125games.jpg",
            "125home.jpg",
            "125items.png",
            "125logo2.jpg",
            "125logo.png",
            "125make.png",
            "125pink.jpg",
            "125play.png",
            "125see.png",
            "125tofu.png",
            "125trophies.png",
        };
        String[] gameImageFiles = {
            "g_bingo.jpg",
            "g_brawl.jpg",
            "g_corpse.jpg",
            "g_ghost.jpg",
            "g_lol.jpg",
            "g_THD.jpg",
        };
        String[] groupImageFiles = {
            "gr_arcade.jpg",
            "gr_bella.jpg",
            "gr_kawaii.jpg",
        };
        String[] tinyImageFiles = {
            "tb_blue.png",
            "tb_green.png",
            "tb_grey.png",
            "tb_orange.png",
            "tb_pink.png",
            "tb_red.png",
        };

        addLogoGrid(_msgs.linkToWhirledSquareImages(), squareImageFiles, 4);
        add(MsoyUI.createHTML("<hr>", null));
        addLogoGrid(_msgs.linkToWhirledTinyImages(), tinyImageFiles, 3);
        add(MsoyUI.createHTML("<hr>", null));
        addLogoGrid(_msgs.linkToWhirledGameImages(), gameImageFiles, 3);
        add(MsoyUI.createHTML("<hr>", null));
        addLogoGrid(_msgs.linkToWhirledGroupImages(), groupImageFiles, 3);
    }

    protected void addLogoGrid (String title, String[] imagePaths, int columns)
    {
        add(MsoyUI.createLabel(title, "ImageGridLabel"));
        SmartTable grid = new SmartTable(0, 0);
        grid.setWidth("100%");
        for (int ii = 0; ii < imagePaths.length; ++ii) {
            String imagePath = _imageBase + imagePaths[ii];
            FlowPanel imagePanel = new FlowPanel();
            imagePanel.setStyleName("LogoImagePanel");
            Image image = new Image(imagePath);
            image.setStyleName("LogoImage");
            imagePanel.add(image);

            final String embed = "<a href=\"" + SharePanel.getAffiliateLandingUrl(Pages.LANDING) +
                "\"><img src=\"" + DeploymentConfig.serverURL + imagePath.substring(1) + "\"></a>";
            image.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _htmlCode.setText(embed);
                }
            });

            grid.setWidget(ii / columns, ii % columns, imagePanel, 1, "ImageGridCell");
        }
        add(grid);
    }

    protected TextArea _htmlCode;

    protected static final String _imageBase = "/images/people/links/";
    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
