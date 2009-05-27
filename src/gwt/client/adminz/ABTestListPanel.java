//
// $Id$

package client.adminz;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;

import client.ui.MsoyUI;
import client.util.PageCallback;
import client.util.ServiceUtil;

/**
 * Display a dialog for admins to issue invitations to the player base.
 */
public class ABTestListPanel extends FlowPanel
{
    public ABTestListPanel ()
    {
        setStyleName("abTestListPanel");
        add(MsoyUI.createNowLoading());
        _adminsvc.getABTests(new PageCallback<List<ABTest>>(this) {
            public void onSuccess (List<ABTest> tests) {
                init(tests);
            }
        });
    }

    protected void init (List<ABTest> tests)
    {
        clear();
        if (tests.size() == 0) {
            add(MsoyUI.createLabel("No tests", "infoLabel"));
            return;
        }

        SmartTable contents = new SmartTable("Tests", 10, 10);
        add(contents);

        // header row
        int col = 0;
        contents.setText(0, col++, _msgs.abTestName());
        contents.setText(0, col++, _msgs.abTestStarted());
        contents.setText(0, col++, _msgs.abTestEnded());
        contents.getRowFormatter().addStyleName(0, "Header");

        for (final ABTest test : tests) {
            int row = contents.getRowCount();
            col = 0;
            contents.setText(row, col++, test.name);
            contents.setText(row, col++, MsoyUI.formatDate(test.started));
            contents.setText(row, col++, MsoyUI.formatDate(test.ended));
        }
    }

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
