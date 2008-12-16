//
// $Id$

package client.me;

import java.util.Date;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.data.all.Award;

import client.ui.MsoyUI;

public class AwardDisplay extends FlowPanel
{
    /**
     * Empty constructor for subclasses that don't actually use Awards
     */
    public AwardDisplay ()
    {
        setStyleName("awardDisplay");
    }

    public AwardDisplay (Award award)
    {
        super();

        addIcon(award.icon.getMediaPath());
        addName(award.name);
        addDescription(award.description);
        if (award.whenEarned > 0) {
            addEarnedDate(new Date(award.whenEarned));
        }
    }

    protected void addIcon (String url)
    {
        add(MsoyUI.createImage(url, "AwardIcon"));
    }

    protected void addName (String name)
    {
        add(MsoyUI.createLabel(name, "AwardName"));
    }

    protected void addDescription (String description)
    {
        add(MsoyUI.createHTML(description, "AwardDescription"));
    }

    protected void addEarnedDate (Date earnedDate)
    {
        String whenEarned = _msgs.passportFinishedSeries(MsoyUI.formatDate(earnedDate));
        add(MsoyUI.createLabel(whenEarned, "WhenEarned"));
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
