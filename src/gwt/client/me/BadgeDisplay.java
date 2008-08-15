//
// $Id$

package client.me;

import java.util.Date;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;

import client.shell.DynamicMessages;
import client.ui.MsoyUI;

class BadgeDisplay extends VerticalPanel
{
    public BadgeDisplay (Badge badge)
    {
        buildBasics(badge);

        if (badge instanceof EarnedBadge) {
            addEarnedBits((EarnedBadge) badge);
        } else if (badge instanceof InProgressBadge) {
            addInProgressBits((InProgressBadge) badge);
        }
    }

    protected void buildBasics (Badge badge)
    {
        HorizontalPanel imageRow = new HorizontalPanel();
        imageRow.add(MsoyUI.createImage(badge.imageUrl(), null));
        add(imageRow);
        imageRow.add(_nameColumn = new VerticalPanel());
        _nameColumn.add(MsoyUI.createLabel(
            _dmsgs.getString("badge_" + Integer.toHexString(badge.badgeCode)), null));
    }

    protected void addEarnedBits (EarnedBadge badge)
    {
        if (badge.whenEarned == null) {
            return;
        }

        Date earnedDate = new Date(badge.whenEarned);
        add(MsoyUI.createLabel(
            _msgs.passportFinishedSeries(MsoyUI.createDateString(earnedDate)), null));
    }

    protected void addInProgressBits (InProgressBadge badge)
    {
        _nameColumn.add(MsoyUI.createLabel("" + badge.coinReward, null));
        add(new ProgressBar(badge.progress));
    }

    protected static class ProgressBar extends HorizontalPanel
    {
        public ProgressBar (float progress)
        {
            // TEMP
            add(MsoyUI.createLabel("progress: " + progress, null));
        }
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);

    protected VerticalPanel _nameColumn;
}
