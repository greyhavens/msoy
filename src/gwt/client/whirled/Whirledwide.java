//
// $Id: MyWhirled.java 5569 2007-08-21 20:44:02Z nathan $

package client.whirled;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;

public class Whirledwide extends FlexTable
{
    public Whirledwide ()
    {
        buildUi();
    }

    protected void buildUi ()
    {
        int row = 0;

        setCellPadding(0);
        setCellSpacing(0);
        
        // TESTING
        setWidget(row++, 0, new Image("/images/whirled/featured_places.jpg"));
    }
}
