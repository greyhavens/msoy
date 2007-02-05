//
// $Id$

package client.profile;

import client.util.FlashClients;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;

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
    protected void didInit (Object blurbData)
    {
        setHeader("Neighborhood");
        _content.setWidget(FlashClients.createNeighborhood((String) blurbData, "480", "360"));
    }

    // @Override // from Blurb
    protected void didFail (String cause)
    {
        setHeader("Error");
        setStatus("Failed to load neighborhood: " + cause);
    }
 
    protected void setStatus (String text)
    {
        _content.setWidget(new Label(text));
    }
    
    protected SimplePanel _content;
}
