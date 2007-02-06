//
// $Id$

package client.item;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.InlineLabel;

/**
 * Fetches and displays the tag cloud for a given item type.
 */
public class TagCloud extends DockPanel
{
    public interface TagCloudListener
    {
        /**
         * Let the listener know that a tag has been chosen, or cleared if the argument is null.
         */
        void tagClicked(String tag);
    }
    public TagCloud (byte type)
    {
        this(type, null);
    }
    
    public TagCloud (byte type, TagCloudListener listener)
    {
        _type = type;
        _listener = listener;
        _tagFlow = new FlowPanel();
        add(_tagFlow, DockPanel.CENTER);
        FlowPanel currentLine = new FlowPanel();
        currentLine.add(new InlineLabel(CItem.imsgs.currentTag()));
        _tagCurrent = new Label();
        _tagCurrent.setStyleName("tagSize3");
        _tagCurrent.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                _listener.tagClicked(null);
            }
        });
        currentLine.add(_tagCurrent);
        add(currentLine, DockPanel.SOUTH);
        setStyleName("tagContents");
        CItem.catalogsvc.getPopularTags(_type, 20, new TagCallback());
    }
    
    public void setListener (TagCloudListener listener)
    {
        _listener = listener;
    }

    public void setCurrentTag (String tag)
    {
        _tagCurrent.setText(tag == null ? "" : tag);
    }
    
    protected class TagCallback implements AsyncCallback
    {
        public void onSuccess (Object result) {
            HashMap _tagMap = (HashMap) result;
            if (_tagMap.size() == 0) {
                _tagCurrent.setText(CItem.imsgs.msgNoTags());
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

            _tagFlow.clear();
            _tagFlow.add(new InlineLabel(CItem.imsgs.cloudCommonTags()));

            for (int ii = 0; ii < _sortedTags.length; ii ++) {
                if (ii > 0) {
                    _tagFlow.add(new InlineLabel(", "));
                }
                final String tag = (String) _sortedTags[ii];
                int count = ((Integer)_tagMap.get(tag)).intValue();
                double rate = ((double) count) / _maxTagCount;
                // let's start with just 4 different tag sizes
                int size = 1+(int)(4 * rate);
                Label label = new Label(tag);
                label.setStyleName("tagSize" + size);
                label.addClickListener(new ClickListener() {
                    public void onClick (Widget widget) {
                        _listener.tagClicked(tag);
                    }
                });
                _tagFlow.add(label);
            }
        }

        public void onFailure (Throwable caught) {
            CItem.log("getPopularTags failed", caught);
            _tagFlow.clear();
            _tagCurrent.setText(CItem.serverError(caught));
        }
    }

    protected byte _type;
    protected FlowPanel _tagFlow;
    protected Label _tagCurrent;
    protected TagCloudListener _listener;
}
