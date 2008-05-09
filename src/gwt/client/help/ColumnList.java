//
// $Id: Marquee.java 8844 2008-04-15 17:05:43Z nathan $

package client.help;

import java.util.List;

import com.google.gwt.user.client.ui.HTML;
import com.threerings.gwt.ui.SmartTable;

/**
 * Displays the list of HTML values evenly in a table with a set number of columns
 */
public class ColumnList extends SmartTable
{
    public ColumnList (List htmlList, int numColumns)
    {
        super(0, 0);
        if (htmlList == null || htmlList.size() == 0 || numColumns == 0) {
            return;
        }
        int itemsPerColumn = new Double(Math.ceil(new Double(htmlList.size()).doubleValue() / new Double(numColumns).doubleValue())).intValue();
        int listIndex = 0;
        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < itemsPerColumn; j++) {
                if (listIndex >= htmlList.size()) {
                    break;
                }
                setWidget(j, i, new HTML((String)htmlList.get(listIndex)));
                listIndex++;
            }
        }
    }
}
