//
// $Id$

package client.people;

import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.badge.data.all.EarnedBadge;

import com.threerings.msoy.profile.gwt.ProfileService;

import client.shell.Args;
import client.shell.DynamicLookup;
import client.shell.Pages;
import client.ui.MsoyUI;

/**
 * Displays a member's recently earned trophies.
 */
public class StampsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.stamps != null && pdata.stamps.size() > 0);
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(_msgs.stampsTitle());

        HorizontalPanel stamps = new HorizontalPanel();
        for (EarnedBadge badge : pdata.stamps) {
            stamps.add(new SimpleBadgeDisplay(badge));
        }
        setContent(stamps);

        setFooterLink(_msgs.seeAll(), Pages.ME, Args.compose("passport", pdata.name.getMemberId()));
    }

    protected static class SimpleBadgeDisplay extends VerticalPanel
    {
        public SimpleBadgeDisplay (EarnedBadge badge)
        {
            setStyleName("SimpleBadgeDisplay");
            setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
            add(MsoyUI.createImage(badge.imageUrl(), "BadgeImage"));
            String badgeName =  Integer.toHexString(badge.badgeCode);
            try {
                badgeName = _dmsgs.xlate("badge_" + badgeName);
            } catch (MissingResourceException mre) {
                // displaying the hex code is the failure case - make sure to test all new badges
                // before letting them out to production.
            }
            add(MsoyUI.createLabel(badgeName, "BadgeName"));
        }
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
