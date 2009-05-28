//
// $Id$

package client.adminz;

import java.util.List;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.web.gwt.Pages;

import client.util.Link;
import client.ui.MsoyUI;

/**
 * Display a dialog for admins to issue invitations to the player base.
 */
public class ABTestListPanel extends AdminDataPanel<List<ABTest>>
{
    public ABTestListPanel ()
    {
        super("abTestList");
        _adminsvc.getABTests(createCallback());
    }

    @Override // from AdminDataPanel
    protected void init (List<ABTest> tests)
    {
        if (tests.size() == 0) {
            addNoDataMessage("No tests.");
            return;
        }

        SmartTable contents = new SmartTable("Tests", 5, 0);
        add(contents);

        // header row
        int col = 0;
        contents.setText(0, col++, _msgs.abTestName());
        contents.setText(0, col++, _msgs.abTestStarted());
        contents.setText(0, col++, _msgs.abTestEnded());
        contents.getRowFormatter().addStyleName(0, "Header");

        for (final ABTest test : tests) {
            int row = contents.addWidget(
                Link.create(test.name, Pages.ADMINZ, "test", test.testId), 1, null);
            col = 1;
            contents.setText(row, col++, MsoyUI.formatDate(test.started));
            contents.setText(row, col++, MsoyUI.formatDate(test.ended));
        }
    }
}
