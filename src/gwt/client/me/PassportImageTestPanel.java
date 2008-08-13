package client.me;

import java.util.List;
import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;

import client.shell.DynamicMessages;
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
        return new BadgeDisplay(badge);
    }

    @Override
    protected String getEmptyMessage() {
        return "ZOMG! The server gave us no badges! Run away!!!!!11one!";
    }

    protected static class BadgeDisplay extends VerticalPanel
    {
        public BadgeDisplay (Badge badge)
        {
            DOM.setStyleAttribute(getElement(), "padding", "10px");

            String type = "UNKNOWN BADGE TYPE";
            String level = "LEVEL UNKNOWN";
            if (badge instanceof EarnedBadge) {
                type = "EarnedBadge";
                level = "Level: " + ((EarnedBadge)badge).level;
            } else if (badge instanceof InProgressBadge) {
                type = "InProgressBadge";
                level = "Next Level: " + ((InProgressBadge)badge).nextLevel;
            }

            String name = Integer.toHexString(badge.badgeCode);
            try {
                name = _dmsgs.getString("badge_" + name);
            } catch (MissingResourceException mre) {
                // nada, default already set.
            }

            add(MsoyUI.createLabel(name, null));
            add(MsoyUI.createLabel(type, null));
            add(MsoyUI.createLabel(level, null));
            add(MsoyUI.createImage(badge.imageUrl(), null));
        }
    }

    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);

    protected static final int GRID_ROWS = 4;
    protected static final int GRID_COLUMNS = 4;
}
