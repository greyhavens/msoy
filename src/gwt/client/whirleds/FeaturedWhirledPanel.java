//
// $Id$

package client.whirleds;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.data.GroupCard;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.shell.WorldClient;
import client.util.MsoyUI;

/**
 * Displays info on a featured Whirled.
 */
public class FeaturedWhirledPanel extends VerticalPanel
{
    public FeaturedWhirledPanel (GroupCard[] whirleds)
    {
        setStyleName("featuredWhirled");
        _whirleds = whirleds;

        add(_flashPanel = new SimplePanel());

        add(_info = new SmartTable(0, 5));
        _info.setWidth("100%");
        _info.setWidget(0, 0, MsoyUI.createPrevNextButtons(new ClickListener() {
            public void onClick (Widget sender) {
                showWhirled((_selidx+_whirleds.length+1) % _whirleds.length);
            }
        }, new ClickListener() {
            public void onClick (Widget sender) {
                showWhirled((_selidx+1) % _whirleds.length);
            }
        }));

        showWhirled(0);
    }

    protected void showWhirled (int index)
    {
        final GroupCard group = _whirleds[_selidx = index];
        WorldClient.displayFeaturedPlace(group.homeSceneId, _flashPanel);

        int col = 1;
        Widget link = Application.groupViewLink(group.name.toString(), group.name.getGroupId());
        _info.setWidget(0, col++, link, 1, "Name");
        _info.getFlexCellFormatter().setHorizontalAlignment(0, col, HasAlignment.ALIGN_RIGHT);
        _info.setWidget(0, col++, new Button(CWhirleds.msgs.featuredEnter(), new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.WORLD, "g" + group.name.getGroupId());
            }
        }));
    }

    protected GroupCard[] _whirleds;
    protected int _selidx;

    protected SmartTable _info;
    protected SimplePanel _flashPanel;

    protected static final SimpleDateFormat EST_FMT = new SimpleDateFormat("MMM dd, yyyy");
}
