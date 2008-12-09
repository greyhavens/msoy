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

        addLogoGrid(_msgs.linkToWhirledSquareImages(), 4,
            new Logo("125ad.jpg", Pages.LANDING),
            new Logo("125create.png", Pages.SHOP),
            new Logo("125design.jpg", Pages.LANDING),
            new Logo("125earn.png", Pages.SHOP),
            new Logo("125findme.jpg", Pages.LANDING),
            new Logo("125games.jpg", Pages.GAMES),
            new Logo("125home.jpg", Pages.LANDING),
            // TODO: go to creator's shop if we support it (currently searching by creator defaults
            // to showing avatars, which the creator may not have any of)
            new Logo("125items.png", Pages.SHOP),
            new Logo("125logo2.jpg", Pages.LANDING),
            new Logo("125logo.png", Pages.LANDING),
            new Logo("125make.png", Pages.LANDING),
            new Logo("125pink.jpg", Pages.LANDING),
            new Logo("125play.png", Pages.GAMES),
            new Logo("125see.png", Pages.ROOMS),
            new Logo("125tofu.png", Pages.LANDING),
            new Logo("125trophies.png", Pages.GAMES));

        add(MsoyUI.createHTML("<hr>", null));

        addLogoGrid(_msgs.linkToWhirledTinyImages(), 3,
            new Logo("tb_blue.png", Pages.LANDING),
            new Logo("tb_green.png", Pages.LANDING),
            new Logo("tb_grey.png", Pages.LANDING),
            new Logo("tb_orange.png", Pages.LANDING),
            new Logo("tb_pink.png", Pages.LANDING),
            new Logo("tb_red.png", Pages.LANDING));

        add(MsoyUI.createHTML("<hr>", null));

        addLogoGrid(_msgs.linkToWhirledGameImages(), 3,
            // hard wired production game ids
            new Logo("g_bingo.jpg", Pages.GAMES, "d", 929),
            new Logo("g_brawl.jpg", Pages.GAMES, "d", 10),
            new Logo("g_corpse.jpg", Pages.GAMES, "d", 827),
            new Logo("g_ghost.jpg", Pages.GAMES, "d", 206),
            new Logo("g_lol.jpg", Pages.GAMES, "d", 7),
            new Logo("g_THD.jpg", Pages.GAMES, "d", 107));

        add(MsoyUI.createHTML("<hr>", null));

        addLogoGrid(_msgs.linkToWhirledGroupImages(), 3,
            // hard wired production group ids
            new Logo("gr_arcade.jpg", Pages.GROUPS, "d", 10),
            new Logo("gr_bella.jpg", Pages.GROUPS, "d", 5),
            new Logo("gr_kawaii.jpg", Pages.GROUPS, "d", 8));
    }

    protected void addLogoGrid (String title, int columns, Logo ...logos)
    {
        add(MsoyUI.createLabel(title, "ImageGridLabel"));
        SmartTable grid = new SmartTable(0, 0);
        grid.setWidth("100%");
        for (int ii = 0; ii < logos.length; ++ii) {
            String imagePath = _imageBase + logos[ii].getImage();
            FlowPanel imagePanel = new FlowPanel();
            imagePanel.setStyleName("LogoImagePanel");
            Image image = new Image(imagePath);
            image.setStyleName("LogoImage");
            image.addStyleName("actionLabel");
            imagePanel.add(image);

            final String embed = "<a href=\"" + logos[ii].getUrl() + "\"><img src=\"" +
                DeploymentConfig.serverURL + imagePath.substring(1) + "\"></a>";
            image.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _htmlCode.setText(embed);
                    _htmlCode.setFocus(true);
                }
            });

            grid.setWidget(ii / columns, ii % columns, imagePanel, 1, "ImageGridCell");
        }
        add(grid);
    }

    protected class Logo
    {
        public Logo (String image, Pages page, Object ...args)
        {
            _image = image;
            _url = SharePanel.getAffiliateLandingUrl(page, args);
        }

        public String getImage ()
        {
            return _image;
        }

        public String getUrl ()
        {
            return _url;
        }

        protected String _image;
        protected String _url;
    }

    protected TextArea _htmlCode;

    protected static final String _imageBase = "/images/people/links/";
    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
