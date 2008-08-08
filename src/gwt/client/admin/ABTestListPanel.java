//
// $Id: IssueInvitesDialog.java 9643 2008-06-30 21:36:32Z nathan $

package client.admin;

import java.util.List;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.web.client.WebMemberService;
import com.threerings.msoy.web.client.WebMemberServiceAsync;

import client.shell.ShellMessages;
import client.shell.TrackingCookie;
import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Display a dialog for admins to issue invitations to the player base.
 */
public class ABTestListPanel extends FlowPanel
{
    public ABTestListPanel ()
    {
        setStyleName("abTestListPanel");
        refresh();
    }

    /**
     * Fetch and display a list of all tests with the newest tests at the top.
     */
    public void refresh ()
    {
        clear();
        Button createButton = new Button(_msgs.abTestCreateNew());
        createButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                new ABTestEditorDialog(null, ABTestListPanel.this).show();
            }
        });
        add(createButton);

        add(_contents = new SmartTable("Tests", 10, 0));

        _adminsvc.getABTests(new MsoyCallback<List<ABTest>>() {
            public void onSuccess (List<ABTest> tests) {
                displayTests(tests);
            }
        });
    }

    /**
     * Print a table with one test per row
     */
    protected void displayTests (List<ABTest> tests)
    {
        if (tests.size() == 0) {
            _contents.setText(0, 0, "No tests");
            return;
        }

        // header row
        int col = 0;
        _contents.setWidget(0, col++, new Label(_msgs.abTestName()));
        _contents.setWidget(0, col++, new Label(_msgs.abTestEnabled()));
        _contents.setWidget(0, col++, new Label(_msgs.abTestStarted()));
        _contents.setWidget(0, col++, new Label(_msgs.abTestEnded()));
        _contents.setWidget(0, col++, new Label(""));
        _contents.getRowFormatter().addStyleName(0, "Header");

        for (final ABTest test : tests) {
            int row = _contents.getRowCount();
            col = 0;
            _contents.setWidget(row, col++, new Label(test.name));
            _contents.setWidget(row, col++, new Label(String.valueOf(test.enabled)));
            _contents.setWidget(row, col++, new Label(
                                    test.started != null ? _dfmt.format(test.started) : ""));
            _contents.setWidget(row, col++, new Label(
                                    test.ended != null ? _dfmt.format(test.ended) : ""));

            Button editButton = new Button(_cmsgs.edit());
            editButton.addClickListener(new ClickListener() {
                public void onClick (Widget widget) {
                    new ABTestEditorDialog(test, ABTestListPanel.this).show();
                }
            });

            Button testButton = new Button("Test");
            testButton.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _membersvc.getABTestGroup(
                        TrackingCookie.get(), test.name, true, new MsoyCallback<Integer>() {
                            public void onSuccess (Integer group) {
                                MsoyUI.info("You are in group #" + group);
                            }
                    });
                }
            });
            testButton.addClickListener(
                MsoyUI.createTestTrackingListener("ClickedTestButton_"+test.name, test.name));
            _contents.setWidget(row, col++, MsoyUI.createButtonPair(editButton, testButton));

        }
    }

    protected FlexTable _contents;

    protected static final SimpleDateFormat _dfmt = new SimpleDateFormat("yyyy-MM-dd");
    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
