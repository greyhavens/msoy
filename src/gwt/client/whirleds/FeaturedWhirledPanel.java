//
// $Id$

package client.whirleds;

import com.google.gwt.user.client.ui.ClickListener;
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

/**
 * Displays info on a featured Whirled.
 */
public class FeaturedWhirledPanel extends VerticalPanel
{
    public FeaturedWhirledPanel (final Group group)
    {
        MsoyUI.makeBox(this, "people", CWhirleds.msgs.featuredTitle());
        SmartTable contents = new SmartTable("featuredWhirled", 0, 5);
        add(contents);

        // display our screenshot in column 1
        contents.setText(0, 0, group.name, 1, "Name");
        Widget shot = MediaUtil.createMediaView(group.getLogo(), MediaDesc.THUMBNAIL_SIZE);
        contents.setWidget(1, 0, shot, 1, "Shot");
        Widget link = Application.createLink(
            CWhirleds.msgs.featuredMoreInfo(), Page.WHIRLEDS, Args.compose("d", group.groupId));
        contents.setWidget(2, 0, link, 1, "MoreInfo");

        // display the game info in column 2
        contents.setText(0, 1, CWhirleds.msgs.featuredOnline("0" /*TODO*/), 1, "Online");
        SmartTable info = new SmartTable(0, 0);
        info.addText(truncate(group.blurb), 2, "Descrip");
        info.addWidget(WidgetUtil.makeShim(5, 5), 2, null);
        int row = info.getRowCount();
        info.setText(row, 0, "Founded:", 1, "BitLabel");
        info.setText(row++, 1, EST_FMT.format(group.creationDate));
        info.setText(row, 0, "Members:", 1, "BitLabel");
        info.setText(row++, 1, ""+group.memberCount);
        contents.setWidget(1, 1, info, 1, "Info");
        contents.getFlexCellFormatter().setRowSpan(1, 1, 2);

        // display enter and info buttons in column 3
        contents.setText(0, 2, "Go:", 1, "Go");
        VerticalPanel buttons = new VerticalPanel();
        buttons.add(MsoyUI.createBigButton("Explore!", new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.WORLD, "g" + group.groupId);
            }
        }));
        contents.setWidget(1, 2, buttons, 1, "Buttons");
    }

    protected static String truncate (String descrip)
    {
        return (descrip.length() <= MAX_DESCRIP_LENGTH) ? descrip :
            descrip.substring(0, MAX_DESCRIP_LENGTH-3) + "...";
    }

    protected static final int MAX_DESCRIP_LENGTH = 100;

    protected static final SimpleDateFormat EST_FMT = new SimpleDateFormat("MMM dd, yyyy");
}
