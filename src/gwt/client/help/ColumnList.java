//
// $Id$

package client.help;

import java.util.Iterator;
import java.util.List;

import com.threerings.gwt.ui.SmartTable;

import client.ui.SafeHTML;

/**
 * Displays the list of HTML values evenly in a table with a set number of columns
 */
public class ColumnList extends SmartTable
{
    public ColumnList (List<String> htmlList, int numColumns)
    {
        super(0, 0);
        if (htmlList == null || htmlList.size() == 0 || numColumns == 0) {
            return;
        }
        int itemsPerColumn = new Double(Math.ceil(new Double(htmlList.size()).doubleValue() /
                             new Double(numColumns).doubleValue())).intValue();
        Iterator<String> iter = htmlList.iterator();
        for (int ii = 0; ii < numColumns; ii++) {
            for (int jj = 0; jj < itemsPerColumn; jj++) {
                if (!iter.hasNext()) {
                    break;
                }
                setWidget(jj, ii, new SafeHTML(iter.next()));
            }
        }
    }
}
