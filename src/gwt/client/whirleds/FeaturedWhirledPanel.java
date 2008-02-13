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
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.MediaUtil;
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
    public FeaturedWhirledPanel (final Group group)
    {
        setStyleName("featuredWhirled");
        MsoyUI.makeBox(this, "people", CWhirleds.msgs.featuredTitle());

        SimplePanel panel = new SimplePanel();
        add(panel);
        WorldClient.displayFeaturedPlace(group.homeSceneId, panel);

        SmartTable info = new SmartTable(0, 5);
        info.setWidth("100%");
        info.setText(0, 0, group.name, 1, "Name");
        Widget link = Application.groupViewLink(CWhirleds.msgs.featuredMoreInfo(), group.groupId);
        info.setWidget(0, 1, link, 1, "MoreInfo");
        info.setWidget(0, 2, new Button(CWhirleds.msgs.featuredEnter(), new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.WORLD, "g" + group.groupId);
            }
        }));
        info.getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
        add(info);

//         Widget link = Application.createLink(
//             CWhirleds.msgs.featuredMoreInfo(), Page.WHIRLEDS, Args.compose("d", group.groupId));
//         info.setWidget(2, 0, link, 1, "MoreInfo");

//         // display the game info in column 2
//         contents.setText(0, 1, CWhirleds.msgs.featuredOnline("0" /*TODO*/), 1, "Online");
//         SmartTable info = new SmartTable(0, 0);
//         info.addWidget(WidgetUtil.makeShim(5, 5), 2, null);
//         int row = info.getRowCount();
//         info.setText(row, 0, "Founded:", 1, "BitLabel");
//         info.setText(row++, 1, EST_FMT.format(group.creationDate));
//         info.setText(row, 0, "Members:", 1, "BitLabel");
//         info.setText(row++, 1, ""+group.memberCount);
//         contents.setWidget(1, 1, info, 1, "Info");
//         contents.getFlexCellFormatter().setRowSpan(1, 1, 2);

//         // display enter and info buttons in column 3
//         contents.setText(0, 2, "Go:", 1, "Go");
//         VerticalPanel buttons = new VerticalPanel();
//         buttons.add();
//         contents.setWidget(1, 2, buttons, 1, "Buttons");
    }

    protected static String truncate (String descrip)
    {
        return (descrip.length() <= MAX_DESCRIP_LENGTH) ? descrip :
            descrip.substring(0, MAX_DESCRIP_LENGTH-3) + "...";
    }

    protected static final int MAX_DESCRIP_LENGTH = 100;

    protected static final SimpleDateFormat EST_FMT = new SimpleDateFormat("MMM dd, yyyy");
}
