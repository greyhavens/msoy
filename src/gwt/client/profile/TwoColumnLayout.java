//
// $Id$

package client.profile;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.web.data.BlurbData;
import com.threerings.msoy.web.data.PersonLayout;

import client.util.WebContext;

/**
 * Lays out a person page in two columns.
 */
public class TwoColumnLayout extends VerticalPanel
{
    public TwoColumnLayout (
        WebContext ctx, int memberId, PersonLayout layout, ArrayList blurbs)
    {
        // TODO: actually do two columns...
        for (int ii = 0; ii < layout.blurbs.size(); ii++) {
            BlurbData bdata = (BlurbData)layout.blurbs.get(ii);
            Blurb blurb = Blurb.createBlurb(bdata.type);
            blurb.init(ctx, memberId, bdata.blurbId, blurbs.get(ii));
            add(blurb);
        }
    }
}
