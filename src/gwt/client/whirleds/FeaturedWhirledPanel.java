//
// $Id$

package client.whirleds;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.data.GroupCard;

import client.shell.Application;
import client.shell.Page;
import client.shell.WorldClient;
import client.util.MsoyUI;

/**
 * Displays info on a featured Whirled.
 */
public class FeaturedWhirledPanel extends FlowPanel
{
    public FeaturedWhirledPanel ()
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
    }

    public void setWhirleds (GroupCard[] whirleds)
    {
        _whirleds = whirleds;
        showWhirled(0);
    }

    protected void showWhirled (int index)
    {
        final GroupCard group = _whirleds[_selidx = index];
        WorldClient.displayFeaturedPlace(group.homeSceneId, _flashPanel);

        Widget link = Application.groupViewLink(group.name.toString(), group.name.getGroupId());
        _info.setWidget(0, 1, link, 1, "Name");
        if (group.population > 0) {
            _info.setText(1, 0, CWhirleds.msgs.featuredOnline(""+group.population), 1, "Online");
        } else {
            _info.setHTML(1, 0, "&nbsp;");
        }
        _info.setText(2, 0, group.blurb, 3, "Blurb");
    }

    protected GroupCard[] _whirleds;
    protected int _selidx;

    protected SmartTable _info;
    protected SimplePanel _flashPanel;
}
