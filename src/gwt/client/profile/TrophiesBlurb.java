//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.web.client.ProfileService;

import client.game.TrophyGrid;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.ContentFooterPanel;

/**
 * Displays a member's recently earned trophies.
 */
public class TrophiesBlurb extends Blurb
{
    // @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.trophies != null && pdata.trophies.size() > 0);
    }

    // @Override // from Blurb
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        setHeader(CProfile.msgs.trophiesTitle());

        FlexTable grid = new FlexTable();
        grid.setCellSpacing(4);
        grid.setCellPadding(0);
        grid.setWidth("100%");

        // display our trophies in a nice grid
        Trophy[] tvec = (Trophy[])pdata.trophies.toArray(new Trophy[pdata.trophies.size()]);
        TrophyGrid.populateTrophyGrid(grid, tvec);

        FlowPanel footer = new FlowPanel();
        String args = Args.compose("t", pdata.name.getMemberId());
        footer.add(Application.createLink(CProfile.msgs.seeAll(), Page.GAME, args));

        ContentFooterPanel content = new ContentFooterPanel(grid, footer);
        content.addStyleName("trophiesBlurb");
        content.getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_LEFT);
        setContent(content);
    }

    protected static final int COLUMNS = 3;
}
