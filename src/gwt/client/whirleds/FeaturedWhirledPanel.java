//
// $Id$

package client.whirleds;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.GroupCard;

import client.shell.Application;
import client.shell.WorldClient;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Displays info on a featured Whirled.
 */
public class FeaturedWhirledPanel extends FlowPanel
{
    public FeaturedWhirledPanel (boolean showThumbnails)
    {
        setStyleName("FeaturedWhirled");
        add(MsoyUI.createLabel(CWhirleds.msgs.featuredTitle(), "Title"));
        add(_flashPanel = new SimplePanel());
        _flashPanel.addStyleName("Flash");

        add(_info = new SmartTable("pagedGrid", 0, 5)); // hijack PagedGrid styles
        _info.setWidth("400px");

        Button prev = new Button(CWhirleds.cmsgs.prev());
        prev.setStyleName("Button");
        prev.addStyleName("PrevButton");
        prev.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                showWhirled((_selidx+_whirleds.length-1) % _whirleds.length);
            }
        });
        _info.setWidget(0, 0, prev);
        _info.getFlexCellFormatter().setRowSpan(0, 0, 2);

        Button next = new Button(CWhirleds.cmsgs.next());
        next.setStyleName("Button");
        next.addStyleName("NextButton");
        next.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                showWhirled((_selidx+1) % _whirleds.length);
            }
        });
        _info.setWidget(0, 2, next);
        _info.getFlexCellFormatter().setRowSpan(0, 2, 2);
        
        // optional list of icons
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
        WorldClient.displayFeaturedPlace(group.homeSceneId, _flashPanel);

        Widget link = Application.groupViewLink(group.name.toString(), group.name.getGroupId());
        _info.setWidget(0, 1, link, 1, "Name");
        if (group.population > 0) {
            Label onlineCount = new Label(CWhirleds.msgs.featuredOnline(""+group.population));
            SimplePanel onlineBox = new SimplePanel();
            onlineBox.setStyleName("OnlineBox");
            onlineBox.add(onlineCount);
            _info.setWidget(1, 0, onlineBox, 1, "Online");
        } else {
            _info.setHTML(1, 0, "&nbsp;");
        }
        _info.setText(2, 0, group.blurb, 3, "Blurb");
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
            Widget image = MediaUtil.createMediaView(card.logo, MediaDesc.HALF_THUMBNAIL_SIZE, groupClick);
            FlowPanel iconBox = new FlowPanel();
            iconBox.setStyleName("IconBox");
            iconBox.add(image);
            add(iconBox);
            add(MsoyUI.createActionLabel(card.name.toString(), groupClick));
        }
    }
    
    protected FlowPanel _iconsPanel; 
    
    protected GroupCard[] _whirleds;
    protected int _selidx;

    protected SmartTable _info;
    protected SimplePanel _flashPanel;
}
