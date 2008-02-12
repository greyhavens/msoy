//
// $Id$

package client.people;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.person.data.BlurbData;
import com.threerings.msoy.web.client.ProfileService;

/**
 * Lays out a profile page in one column.
 */
public class OneColumnLayout extends VerticalPanel
{
    public OneColumnLayout (ProfileService.ProfileResult pdata)
    {
        for (int ii = 0; ii < pdata.layout.blurbs.size(); ii++) {
            BlurbData bdata = (BlurbData)pdata.layout.blurbs.get(ii);
            Blurb blurb = Blurb.createBlurb(bdata.type);
            if (blurb != null) {
                blurb.init(bdata.blurbId, pdata);
                add(blurb);
            } else {
                add(new Label("Unknown blurb type " + bdata.type));
            }
        }
    }
}
