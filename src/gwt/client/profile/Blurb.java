//
// $Id$

package client.profile;

import client.profile.HoodBlurb;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.BlurbData;
import com.threerings.msoy.data.all.MemberName;

/**
 * Contains a chunk of content that a user would want to display on their personal page.
 */
public abstract class Blurb extends FlexTable
{
    /**
     * Creates the appropriate UI for the specified type of blurb.
     */
    public static Blurb createBlurb (int type)
    {
        switch (type) {
        case BlurbData.PROFILE:
            return new ProfileBlurb();
        case BlurbData.FRIENDS:
            return new FriendsBlurb();
        case BlurbData.GROUPS:
            return new GroupsBlurb();
        case BlurbData.HOOD:
            return new HoodBlurb();
        case BlurbData.RATINGS:
            return new RatingsBlurb();
        default:
            return null;
        }
    }

    /**
     * Configures this blurb with a context and the member id for whom it is displaying content.
     */
    public void init (int blurbId, ProfileService.ProfileResult pdata)
    {
        _blurbId = blurbId;
        _name = pdata.name;
        didInit(pdata);
    }

    protected Blurb ()
    {
        setCellPadding(0);
        setCellSpacing(0);
        setStyleName("blurbBox");
        getFlexCellFormatter().setStyleName(0, 0, "HeaderLeft");
        setWidget(0, 1, _header = new Label("Header"));
        getFlexCellFormatter().setStyleName(0, 1, "Header");
        getFlexCellFormatter().setStyleName(0, 2, "HeaderRight");
        setWidget(1, 0, createContent());
        getFlexCellFormatter().setColSpan(1, 0, 3);
        getFlexCellFormatter().setStyleName(1, 0, "Content");
    }

    /**
     * Can be called by the derived class to set the header text of this blurb.
     */
    protected void setHeader (String header)
    {
        _header.setText(header);
    }

    /**
     * Creates the interface components for this blurb. This is called during construction and the
     * blurb will not yet have been initialized.
     */
    protected abstract Panel createContent ();

    /**
     * Called once we have been configured with our context and member info.
     */
    protected abstract void didInit (ProfileService.ProfileResult pdata);

    protected MemberName _name;
    protected int _blurbId;
    protected Label _header;
}
