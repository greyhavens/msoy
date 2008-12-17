//
// $Id$

package client.adminz;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.AdminService.ItemFlagsResult;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.gwt.ItemDetail;

import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

/**
 * An interface for dealing with flagged items: mark them mature if they were flagged thus,
 * or delete them, or simply remove the flags.
 */
public class ReviewPanel extends FlowPanel
{
    public ReviewPanel ()
    {
        setStyleName("reviewPanel");

        RowPanel buttons = new RowPanel();
        Button reloadButton = new Button(_msgs.reviewReload());
        reloadButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                refresh();
            }
        });
        buttons.add(reloadButton);
        add(buttons);

        refresh();
    }

    public ItemDetail getItemDetail (byte type, int id)
    {
        if (_result == null) {
            return null;
        }
        for (ItemDetail detail : _result.items.values()) {
            if (detail.item.getType() == type && detail.item.itemId == id) {
                return detail;
            }
        }
        return null;
    }

    // clears the UI and repopuplates the list
    protected void refresh ()
    {
        if (_contents != null) {
            remove(_contents);
        }
        _contents = new PagedTable<ItemFlag>(5) {
            @Override protected List<Widget> createHeader () {
                List<Widget> header = new ArrayList<Widget>();
                header.add(MsoyUI.createLabel(_msgs.reviewColumnThumbnail(), null));
                header.add(MsoyUI.createLabel(_msgs.reviewColumnComment(), null));
                header.add(MsoyUI.createLabel(_msgs.reviewColumnActions(), null));
                return header;
            }

            @Override protected List<Widget> createRow (ItemFlag item) {
                List<Widget> row = new ArrayList<Widget>();
                ItemDetail detail = _result.items.get(item.itemIdent);

                // thumbnail
                if (detail != null) {
                    row.add(new ThumbBox(detail.item.getThumbnailMedia()));
                } else {
                    row.add(new Label("Item not found"));
                }

                // reporter, comment and timestamp
                row.add(new Comment(item));

                // item actions
                row.add(new ReviewItem(ReviewPanel.this, detail, item));

                return row;
            }

            @Override protected String getEmptyMessage () {
                return _msgs.reviewNoItems();
            }
        };
        _contents.setModel(new ItemFlagsModel(), 0);
        _contents.addStyleName("Table");
        add(_contents);
    }

    protected class Comment extends VerticalPanel
    {
        public Comment (ItemFlag item)
        {
            setStyleName("Comment");
            setSpacing(2);
            MemberName memberName = _result.memberNames.get(item.memberId);
            HorizontalPanel nameTime = new HorizontalPanel();
            nameTime.setSpacing(2);
            nameTime.add(Link.memberView(memberName.toString(), item.memberId));
            nameTime.add(MsoyUI.createLabel(" - ", null));
            nameTime.setStyleName("CommentHeader");
            Label time = MsoyUI.createLabel(MsoyUI.formatDateTime(item.timestamp), "Time");
            time.setWordWrap(false);
            nameTime.add(time);
            add(nameTime);
            if (item.comment.length() > 0) {
                HTML comment = MsoyUI.createRestrictedHTML(item.comment, false);
                comment.setStyleName("CommentText");
                add(comment);
            } else {
                add(MsoyUI.createLabel("Empty", "EmptyCommentText"));
            }
        }
    }

    protected class ItemFlagsModel
        extends ServiceBackedDataModel<ItemFlag, ItemFlagsResult>
    {
        @Override protected void callFetchService (
            int start, int count, boolean needCount, AsyncCallback<ItemFlagsResult> callback)
        {
            _adminsvc.getItemFlags(start, count, needCount, callback);
        }

        @Override protected int getCount (ItemFlagsResult result)
        {
            return result.total;
        }

        @Override protected List<ItemFlag> getRows (ItemFlagsResult result)
        {
            _result = result;
            return result.page;
        }
    }

    protected PagedTable<ItemFlag> _contents;
    protected ItemFlagsResult _result;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
