//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.badge.data.all.Badge;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;

import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * A test page for making sure that the names and images for all badges defined on the server
 * are set up correctly.  This only gets compiled on dev deployments, because the only place
 * it gets created (MePage) only creates it conditionally on DeploymentConfig.devDeployment.
 */
public class PassportImageTestPanel extends PagedGrid<Badge>
{
    public PassportImageTestPanel ()
    {
        super(GRID_ROWS, GRID_COLUMNS);
        _mesvc.loadAllBadges(new MsoyCallback<List<Badge>>() {
            public void onSuccess(List<Badge> badges) {
                setModel(new SimpleDataModel<Badge>(badges), 0);
            }
        });
    }

    @Override
    protected Widget createWidget(Badge badge)
    {
        return MsoyUI.createSimplePanel(new BadgeDisplay(badge), "BoxedAward");
    }

    @Override
    protected String getEmptyMessage() {
        return "ZOMG! The server gave us no badges! Run away!!!!!11one!";
    }

    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);

    protected static final int GRID_ROWS = 4;
    protected static final int GRID_COLUMNS = 4;
}
