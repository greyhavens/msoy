//
// $Id$

package client.item;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

import client.shell.CShell;
import client.util.MsoyUI;

/**
 * Fetches and displays the tag cloud for a given item type.
 */
public class TagCloud extends Grid
{
    public interface TagListener
    {
        /** Called when a tag has been clicked (or cleared if the argument is null). */
        public void tagClicked (String tag);
    }

    public TagCloud (byte type, TagListener listener)
    {
        super(TAG_ROWS, TAG_COLS);
        setStyleName("tagCloud");

        HTMLTable.CellFormatter cellFormatter = getCellFormatter();
        HTMLTable.RowFormatter rowFormatter = getRowFormatter();
        for (int row = 0; row < TAG_ROWS; row++) {
            rowFormatter.setStyleName(row, "tagCloudRow");
            for (int col = 0; col < TAG_COLS; col++) {
                cellFormatter.setStyleName(row, col, "tagCloudElt");
            }
        }

        _type = type;
        _listener = listener;
        CShell.catalogsvc.getPopularTags(_type, TAG_ROWS * TAG_COLS, new TagCallback());
    }

    protected class TagCallback implements AsyncCallback
    {
        public void onSuccess (Object result) {
            clear();

            HashMap _tagMap = (HashMap) result;
            if (_tagMap.size() == 0) {
                setText(0, 0, CShell.imsgs.msgNoTags());
                return;
            }

            // figure out the highest use count among all the tags
            Iterator vIter = _tagMap.values().iterator();
            int _maxTagCount = 0;
            while (vIter.hasNext()) {
                int count = ((Integer) vIter.next()).intValue();
                if (count > _maxTagCount) {
                    _maxTagCount = count;
                }
            }

            // then sort the tag names
            Object[] _sortedTags = _tagMap.keySet().toArray();
            Arrays.sort(_sortedTags);

            for (int row = 0; row < TAG_ROWS; row++) {
                for (int col = 0; col < TAG_COLS; col++) {
                    int idx = (row * TAG_COLS + col);
                    if (idx >= _sortedTags.length) {
                        clearCell(row, col);
                        continue;
                    }

//                     int count = ((Integer)_tagMap.get(tag)).intValue();
//                     double rate = ((double) count) / _maxTagCount;
//                     // let's start with just 4 different tag sizes
//                     int size = 1+(int)(4 * rate);

                    final String tag = (String) _sortedTags[idx];
                    setWidget(row, col, MsoyUI.createActionLabel(tag, new ClickListener() {
                        public void onClick (Widget widget) {
                            _listener.tagClicked(tag);
                        }
                    }));
                }
            }
        }

        public void onFailure (Throwable caught) {
            CShell.log("getPopularTags failed", caught);
            clear();
            setText(0, 0, CShell.serverError(caught));
        }
    }

    protected byte _type;
    protected TagListener _listener;

    protected static final int TAG_ROWS = 3;
    protected static final int TAG_COLS = 3;
}
