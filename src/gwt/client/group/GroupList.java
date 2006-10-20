//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;

import client.MsoyEntryPoint;
import client.group.GroupEdit.GroupSubmissionListener;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.Group;

/**
 * Display all existing groups in a simple list format. This will change when
 * there is a non-trivial number of groups.
  */
public class GroupList extends DockPanel
    implements GroupSubmissionListener
{
    public GroupList(WebContext ctx)
    {
        super();
        _ctx = ctx;
        
        _table = new FlexTable();
        _table.setStyleName("groupList");
        add(_table, DockPanel.CENTER);

        HorizontalPanel bpanel = new HorizontalPanel();
        add(bpanel, DockPanel.SOUTH);

        Button editButton = new Button("Create New");
        bpanel.add(editButton);
        editButton.setStyleName("groupEditorButton");
        editButton.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                new GroupEdit(_ctx, new Group(), GroupList.this).show();
            }
        });
        
        loadGroups();
    }

    // callback from {@link GroupEditor}
    public void groupSubmitted (Group group)
    {
        // just refresh the whole view
        loadGroups();
    }

    // refetch the data and trigger a UI rebuild
    protected void loadGroups ()
    {
        _ctx.groupsvc.getGroups(_ctx.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                populateTable((List) result);
            }
            public void onFailure (Throwable caught) {
                GWT.log("loadGroups failed", caught);
                // TODO: if ServiceException, translate
                // TODO: error report
            }
        });
    }

    // (re)build the group list from scratch
    protected void populateTable (List groups)
    {
        _table.clear();
        int row = 0;

        _table.setText(row, 0, "Name");
        _table.setText(row, 1, "Logo");
        _table.setText(row, 2, "Policy");
        row ++;
        
        Iterator iterator = groups.iterator();
        while (iterator.hasNext()) {
            Group group = (Group)iterator.next();

            // first column: the group's name
            _table.setWidget(row, 0, new Hyperlink(group.name, Integer.toString(group.groupId)));

            // second column: the logo
            Image logo = new Image(group.logo == null ? "/msoy/images/default_logo.png" :
                MsoyEntryPoint.toMediaPath(group.logo.getMediaPath()));
            logo.setStyleName("groupLogoThumbnail");
            _table.setWidget(row, 1, logo);

            // third column: the policy
            _table.setText(row, 2, group.policy == Group.POLICY_PUBLIC ? "Public" :
                    group.policy == Group.POLICY_EXCLUSIVE ? "Exclusive" : "Invitation Only");
            _table.getRowFormatter().setVerticalAlign(row, HasAlignment.ALIGN_TOP);

            row ++;
        }
    }

    protected WebContext _ctx;
    protected FlexTable _table;
}
