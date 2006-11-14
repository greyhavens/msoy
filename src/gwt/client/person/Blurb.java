//
// $Id$

package client.person;

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.BlurbData;

/**
 * Contains a chunk of content that a user would want to display on their
 * personal page.
 */
public abstract class Blurb extends DockPanel
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
        default:
            return null; // TODO: return NOOP blurb?
        }
    }

    /**
     * Configures this blurb with a context and the member id for whom it is
     * displaying content.
     */
    public void init (
        WebContext ctx, int memberId, int blurbId, Object blurbData)
    {
        _ctx = ctx;
        _memberId = memberId;
        _blurbId = blurbId;
        if (blurbData instanceof String) {
            didFail((String)blurbData);
        } else {
            didInit(blurbData);
        }
    }

    protected Blurb ()
    {
        setStyleName("blurb_box");
        add(_header = new Label("Header"), NORTH);
        _header.setStyleName("blurb_header");
        add(createContent(), CENTER);
    }

    /**
     * Can be called by the derived class to set the header text of this blurb.
     */
    protected void setHeader (String header)
    {
        _header.setText(header);
    }

    /**
     * Creates the interface components for this blurb. This is called during
     * construction and the blurb will not yet have been initialized.
     */
    protected abstract Panel createContent ();

    /**
     * Called once we have been configured with our context and member info.
     * The blurb data will be the content to be displayed by this blurb.
     */
    protected abstract void didInit (Object blurbData);

    /**
     * Called if our blurb data failed to load.
     */
    protected abstract void didFail (String cause);

    protected WebContext _ctx;
    protected int _memberId;
    protected int _blurbId;
    protected Label _header;
}
