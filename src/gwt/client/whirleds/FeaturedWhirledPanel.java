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

import client.util.MsoyUI;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.shell.WorldClient;

/**
 * Displays info on a featured Whirled.
 */
public class FeaturedWhirledPanel extends VerticalPanel
{
    public FeaturedWhirledPanel (GroupCard[] whirleds)
    {
        setStyleName("featuredWhirled");

        // TODO: next, prev
        final GroupCard group = whirleds[0];
        SimplePanel panel = new SimplePanel();
        add(panel);
        WorldClient.displayFeaturedPlace(group.homeSceneId, panel);

        SmartTable info = new SmartTable(0, 5);
        info.setWidth("100%");
        info.setText(0, 0, group.name.toString(), 1, "Name");
        Widget link = Application.groupViewLink(
            CWhirleds.msgs.featuredMoreInfo(), group.name.getGroupId());
        info.setWidget(0, 1, link, 1, "MoreInfo");
        info.setWidget(0, 2, new Button(CWhirleds.msgs.featuredEnter(), new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.WORLD, "g" + group.name.getGroupId());
            }
        }));
        info.getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
        add(info);
    }

    protected static String truncate (String descrip)
    {
        return (descrip.length() <= MAX_DESCRIP_LENGTH) ? descrip :
            descrip.substring(0, MAX_DESCRIP_LENGTH-3) + "...";
    }

    protected static final int MAX_DESCRIP_LENGTH = 100;

    protected static final SimpleDateFormat EST_FMT = new SimpleDateFormat("MMM dd, yyyy");
}
