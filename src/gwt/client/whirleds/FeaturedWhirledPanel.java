//
// $Id$

package client.whirleds;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.gwt.GroupCard;

import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.FeaturedPlaceUtil;
import client.util.Link;
import client.util.MediaUtil;

/**
 * Displays info on a featured Whirled.
 */
public class FeaturedWhirledPanel extends FlowPanel
{
    /**
     * Create a new Featured Whirled Panel
     * @param showThumbnails Display a list of featured whirled thumbnails
     * @param showPlaceholder Display an image instead of loading the live room
     */
    public FeaturedWhirledPanel (boolean showThumbnails, boolean showPlaceholder)
    {
        setStyleName("FeaturedWhirled");
        _showPlaceholder = showPlaceholder;
        add(MsoyUI.createLabel(_msgs.featuredTitle(), "Title"));
        add(_flashPanel = MsoyUI.createSimplePanel("Flash", null));
        add(_infoPanel = new FlowPanel());

        // prev & next buttons are re-positioned in the css
        add(MsoyUI.createPrevNextButtons(
            new ClickListener() {
                public void onClick (Widget sender) {
                    showWhirled((_selidx+_whirleds.length-1) % _whirleds.length);
                }
            },
            new ClickListener() {
                public void onClick (Widget sender) {
                    showWhirled((_selidx+1) % _whirleds.length);
                }
            }));

        // optional list of icons of other whirleds
        if (showThumbnails) {
            add(_iconsPanel = new FlowPanel());
            _iconsPanel.setStyleName("Icons");
        }
    }

    public void setWhirleds (GroupCard[] whirleds)
    {
        _whirleds = whirleds;
        showWhirled(0);

        // display icons for the first four whirleds
        if (_iconsPanel != null) {
            for (int i = 0; i < 4; i++) {
                if (i >= _whirleds.length) {
                    break;
                }
                final GroupCard group = _whirleds[i];
                _iconsPanel.add(new IconPanel(group, i));
            }
        }
    }

    protected void showWhirled (int index)
    {
        final GroupCard group = _whirleds[_selidx = index];

        if (!_showPlaceholder) {
            if (!FeaturedPlaceUtil.displayFeaturedPlace(group.homeSceneId, _flashPanel)) {
                return; // client not yet ready to switch scenes
            }

        } else {
            // display screenshot with click to play button
            if (_flashPanel.getWidget() != null) {
                _flashPanel.remove(_flashPanel.getWidget());
            }
            ClickListener onClick = new ClickListener() {
                public void onClick (Widget sender) {
                    _flashPanel.remove(_flashPanel.getWidget());
                    // after the placeholder is removed, do not readd it
                    // _showPlaceholder = false;
                    FeaturedPlaceUtil.displayFeaturedPlace(group.homeSceneId, _flashPanel);
                }
            };

            _flashPanel.add(LiveViewUtil.makeLiveViewWidget(group, onClick));
        }

        _infoPanel.clear();
        Widget nameLink = Link.groupView(group.name.toString(), group.name.getGroupId());
        nameLink.setStyleName("FeaturedWhirledName");
        _infoPanel.add(nameLink);
        if (group.population > 0) {
            Label onlineCount = new Label(_msgs.featuredOnline(""+group.population));
            SimplePanel onlineBox = new SimplePanel();
            onlineBox.setStyleName("OnlineBox");
            onlineBox.add(onlineCount);
            _infoPanel.add(onlineBox);
        }
        _infoPanel.add(MsoyUI.createHTML(group.blurb, "Blurb"));
    }

    /**
     * A widget containing a the group icon and name.
     */
    protected class IconPanel extends FlowPanel
    {
        public IconPanel (final GroupCard card, final int index) {
            setStyleName("Icon");
            ClickListener groupClick = new ClickListener() {
                public void onClick (Widget sender) {
                    showWhirled(index);
                }
            };

            // put the icon in a box with the whirled name beneath it
            Widget image = MediaUtil.createMediaView(
                card.logo, MediaDesc.HALF_THUMBNAIL_SIZE, groupClick);
            FlowPanel iconBox = new FlowPanel();
            iconBox.setStyleName("IconBox");
            iconBox.add(image);
            add(iconBox);
            add(MsoyUI.createActionLabel(card.name.toString(), groupClick));
        }
    }

    protected boolean _showPlaceholder;
    protected FlowPanel _iconsPanel;
    protected GroupCard[] _whirleds;
    protected int _selidx;
    protected FlowPanel _infoPanel;
    protected SimplePanel _flashPanel;

    protected static final WhirledsMessages _msgs = GWT.create(WhirledsMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
