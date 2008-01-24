//
// $Id$

package client.remix;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

public class DataPackCreationPanel extends FlexTable
{
    public DataPackCreationPanel ()
    {
        Button create = new Button(CRemix.rmsgs.createNew(), new ClickListener() {
            public void onClick (Widget sender) {
                // TODO
            }
        });
        // TODO
    }
}
