//
// $Id$

package client.me;

import java.util.Date;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.data.all.Award;

import client.ui.MsoyUI;

public class AwardDisplay extends FlowPanel
{
    /**
     * Empty constructor for subclasses that don't actually use Awards
     *
     * @param clicker If non-null, will be attached to the name and icon of the award.
     */
    public AwardDisplay (ClickListener clicker)
    {
        setStyleName("awardDisplay");
        _clicker = clicker;
    }

    public AwardDisplay (Award award)
    {
        this(award, null);
    }

    public AwardDisplay (Award award, ClickListener clicker)
    {
        this(clicker);

        addIcon(award.icon.getMediaPath());
        addName(award.name);
        addDescription(award.description);
        if (award.whenEarned > 0) {
            addEarnedDate(new Date(award.whenEarned));
        }
    }

    protected void addIcon (String url)
    {
        add(MsoyUI.makeActionImage(MsoyUI.createImage(url, "AwardIcon"), null, _clicker));
    }

    protected void addName (String name)
    {
        add(MsoyUI.createActionLabel(name, "AwardName", _clicker));
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

    protected ClickListener _clicker;

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
