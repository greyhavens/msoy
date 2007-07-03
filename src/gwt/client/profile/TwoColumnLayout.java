//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.BlurbData;

/**
 * Lays out a profile page in two columns.
 */
public class TwoColumnLayout extends FlexTable
{
    public TwoColumnLayout (ProfileService.ProfileResult pdata)
    {
        setCellPadding(5);
        for (int ii = 0; ii < pdata.layout.blurbs.size(); ii++) {
            BlurbData bdata = (BlurbData)pdata.layout.blurbs.get(ii);
            Blurb blurb = Blurb.createBlurb(bdata.type);
            blurb.init(bdata.blurbId, pdata);
            setWidget(ii/2, ii%2, blurb);
            getFlexCellFormatter().setVerticalAlignment(ii/2, ii%2, HasAlignment.ALIGN_TOP);
        }
    }
}
