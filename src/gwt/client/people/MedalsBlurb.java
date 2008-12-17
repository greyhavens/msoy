//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.data.all.Award;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;

public class MedalsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.medals != null && pdata.medals.size() > 0);
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(_msgs.medalsTitle());

        HorizontalPanel medals = new HorizontalPanel();
        for (Award badge : pdata.medals) {
            medals.add(new SimpleMedalDisplay(badge));
        }
        setContent(medals);

        // TODO: add URL linkage to medals view
        setFooterLink(_msgs.seeAll(), Pages.ME, Args.compose("passport", pdata.name.getMemberId()));
    }

    protected static class SimpleMedalDisplay extends VerticalPanel
    {
        public SimpleMedalDisplay (Award medal)
        {
            // hijack styles from the passport stamps blurb
            setStyleName("SimpleBadgeDisplay");
            setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
            add(MsoyUI.createImage(medal.icon.getMediaPath(), "BadgeImage"));
            add(MsoyUI.createLabel(medal.name, "BadgeName"));
        }
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);

}
