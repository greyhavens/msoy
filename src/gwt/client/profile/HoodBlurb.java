//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.web.client.ProfileService;

/**
 * Displays a neighborhood visualization.
 */
public class HoodBlurb extends Blurb
{
    // @Override // from Blurb
    protected Panel createContent ()
    {
        return (_content = new SimplePanel());
    }

    // @Override // from Blurb
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        setHeader("Neighborhood");
//         _content.setWidget(FlashClients.createNeighborhood((String) blurbData, "480", "360"));
    }
 
    protected void setStatus (String text)
    {
        _content.setWidget(new Label(text));
    }
    
    protected SimplePanel _content;
}
