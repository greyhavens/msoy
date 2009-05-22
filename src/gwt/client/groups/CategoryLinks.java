//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays our group categories.
 */
public class CategoryLinks extends FlowPanel
{
    public CategoryLinks ()
    {
        setStyleName("categoryLinks");
        setTag("");
    }

    public void setTag (String active)
    {
        clear();
        add(MsoyUI.createLabel(_msgs.galaxyCategoryTitle(), "CategoryTitle inline"));
        for (String category : CATEGORY_TAGS) {
            String tagStyle = "Link";
            String tag = category.toLowerCase();
            if (tag.equals(active)) {
                tagStyle = "SelectedLink";
            }
            add(Link.create(category, tagStyle, Pages.GROUPS, "tag", 0, tag, false));
        }
    }

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);

    protected static final String[] CATEGORY_TAGS = {
        "Games", "Music", "Dance", "Art", "Flash", "Fashion", "Pets", "Sports", "Humor" };
}
