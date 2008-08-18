//
// $Id$

package client.me;

import java.util.Date;
import java.util.MissingResourceException;

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
        setStyleName("badgeDisplay");
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
        String hexCode = Integer.toHexString(badge.badgeCode);
        String badgeName = hexCode;
        try {
            badgeName = _dmsgs.getString("badge_" + hexCode);
        } catch (MissingResourceException mre) {
            // displaying the hex code is the failure case - make sure to test all new badges
            // before letting them out to production.
        }
        _nameColumn.add(MsoyUI.createLabel(badgeName, "StampName"));

        String badgeDesc = null;
        try {
            // first look for a specific message for this level
            badgeDesc = _dmsgs.getString("badgeDesc" + badge.level + "_" + hexCode);
        } catch (MissingResourceException mre) {
            // No big deal, we'll check for the dynamic version next
        }

        if (badgeDesc == null) {
            try {
                badgeDesc =
                    _dmsgs.getString("badgeDescN_" + hexCode).replace("{0}", badge.levelUnits);
            } catch (MissingResourceException mre) {
                // again, this is a testing failure case - never let a badge make it to production
                // with this in the description field.
                badgeDesc = "MISSING DESCRIPTION [" + hexCode + "]";
            }
        }
        add(MsoyUI.createLabel(badgeDesc, "StampDescription"));
    }

    protected void addEarnedBits (EarnedBadge badge)
    {
        if (badge.whenEarned == null) {
            return;
        }

        Date earnedDate = new Date(badge.whenEarned);
        add(MsoyUI.createLabel(_msgs.passportFinishedSeries(MsoyUI.formatDate(earnedDate)), null));
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

    protected VerticalPanel _nameColumn;

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
