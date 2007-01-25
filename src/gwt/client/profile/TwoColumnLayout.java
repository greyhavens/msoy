//
// $Id$

package client.profile;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.threerings.msoy.web.data.BlurbData;
import com.threerings.msoy.web.data.PersonLayout;

/**
 * Lays out a person page in two columns.
 */
public class TwoColumnLayout extends FlexTable
{
    public TwoColumnLayout (int memberId, PersonLayout layout, ArrayList blurbs)
    {
        setCellPadding(5);
        for (int ii = 0; ii < layout.blurbs.size(); ii++) {
            BlurbData bdata = (BlurbData)layout.blurbs.get(ii);
            Blurb blurb = Blurb.createBlurb(bdata.type);
            blurb.init(memberId, bdata.blurbId, blurbs.get(ii));
            setWidget(ii/2, ii%2, blurb);
            getFlexCellFormatter().setVerticalAlignment(ii/2, ii%2, HasAlignment.ALIGN_TOP);
        }
    }
}
