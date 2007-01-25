//
// $Id$

package client.item;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

/**
 * Fetches and displays the tag cloud for a given item type.
 */
public class TagCloud extends FlowPanel
{
    public TagCloud (byte type)
    {
        _type = type;
        setStyleName("tagContents");
        CItem.catalogsvc.getPopularTags(_type, 20, new TagCallback());
    }

    protected class TagCallback implements AsyncCallback
    {
        public void onSuccess (Object result) {
            HashMap _tagMap = (HashMap) result;
            if (_tagMap.size() == 0) {
                add(new Label(CItem.imsgs.msgNoTags()));
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
            StringBuffer buf = new StringBuffer();
            for (int ii = 0; ii < _sortedTags.length; ii ++) {
                if (ii > 0) {
                    buf.append(", ");
                }
                String tag = (String) _sortedTags[ii];
                int count = ((Integer)_tagMap.get(tag)).intValue();
                double rate = ((double) count) / _maxTagCount;
                // let's start with just 4 different tag sizes
                int size = 1+(int)(4 * rate);
                buf.append("<span class='tagSize" + size + "'>");
                buf.append(tag);
                buf.append("</span>");
            }
            add(new HTML(CItem.imsgs.cloudCommonTags(buf.toString())));
        }

        public void onFailure (Throwable caught) {
            CItem.log("getPopularTags failed", caught);
            clear();
            add(new Label(CItem.serverError(caught)));
        }
    }

    protected byte _type;
}
