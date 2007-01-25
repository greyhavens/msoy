//
// $Id$

package client.profile;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.web.data.BlurbData;
import com.threerings.msoy.web.data.PersonLayout;

/**
 * Lays out a person page in one column.
 */
public class OneColumnLayout extends VerticalPanel
{
    public OneColumnLayout (int memberId, PersonLayout layout, ArrayList blurbs)
    {
        for (int ii = 0; ii < layout.blurbs.size(); ii++) {
            BlurbData bdata = (BlurbData)layout.blurbs.get(ii);
            Blurb blurb = Blurb.createBlurb(bdata.type);
            if (blurb != null) {
                blurb.init(memberId, bdata.blurbId, blurbs.get(ii));
                add(blurb);
            } else {
                add(new Label("Unknown blurb type " + bdata.type));
            }
        }
    }
}
