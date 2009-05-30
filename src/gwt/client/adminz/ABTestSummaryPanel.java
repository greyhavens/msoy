//
// $Id$

package client.adminz;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.i18n.client.NumberFormat;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.ABTestSummary;

import client.ui.MsoyUI;

/**
 * Displays a summary for a particular A/B test.
 */
public class ABTestSummaryPanel extends AdminDataPanel<ABTestSummary>
{
    public ABTestSummaryPanel (int testId)
    {
        super("abTestSummary");
        _adminsvc.getABTestSummary(testId, createCallback());
    }

    @Override // from AdminDataPanel
    protected void init (ABTestSummary test)
    {
        if (test == null) {
            addNoDataMessage(_msgs.tspUnknown());
            return;
        }

        SmartTable info = new SmartTable("Info", 5, 0);
        info.setText(0, 0, _msgs.tspName(test.name, ""+test.testId), 2, "Title");
        if (test.ended == null) {
            addRow(info, _msgs.tspStarted(), MsoyUI.formatDateTime(test.started));
        } else {
            addRow(info, _msgs.tspRan(), _msgs.tspDates(MsoyUI.formatDateTime(test.started),
                                                        MsoyUI.formatDateTime(test.ended)));
        }

        List<String> attrs = new ArrayList<String>();
        if (test.onlyNewVisitors) {
            attrs.add(_msgs.tspOnlyNew());
        }
        if (test.landingCookie) {
            attrs.add(_msgs.tspLanding());
        }
        if (attrs.size() > 0) {
            addRow(info, _msgs.tspFlags(), attrs.toString());
        }
        add(info);

        Set<String> actions = new TreeSet<String>();
        for (ABTestSummary.Group group : test.groups) {
            actions.addAll(group.actions.keySet());
        }

        SmartTable results = new SmartTable("Results", 5, 0);
        int col = 0;
        if (test.groups.size() > 0) {
            results.setText(0, col++, _msgs.tspGroup(), 1, "Header");
            results.setText(0, col++, _msgs.tspAssigned(), 1, "Header");
            for (String action : actions) {
                results.setText(0, col++, action, 1, "Header");
            }
            results.setText(0, col++, _msgs.tspRegistered(), 1, "Header");
            results.setText(0, col++, _msgs.tspRetained(), 1, "Header");
        } else {
            results.setText(0, 0, _msgs.tspNoGroupData());
        }

        for (ABTestSummary.Group group : test.groups) {
            int row = results.addText(""+group.group, 1, "rightLabel");
            col = 1;
            results.setText(row, col++, ""+group.assigned, 1, "rightLabel");
            for (String action : actions) {
                Integer takers = group.actions.get(action);
                results.setText(row, col++, format(takers == null ? 0 : takers, group.assigned),
                                1, "rightLabel");
            }
            results.setText(row, col++, format(group.registered, group.assigned), 1, "rightLabel");
            results.setText(row, col++, format(group.retained, group.assigned), 1, "rightLabel");
        }

        add(results);
    }

    protected String format (int value, int total)
    {
        StringBuilder result = new StringBuilder().append(value);
        if (value > 0 && total > 0) {
            result.append(" (").append(_fmt.format(value/(float)total)).append(")");
        }
        return result.toString();
    }

    protected int addRow (SmartTable table, String label, Object data)
    {
        int row = table.addText(label, 1, "rightLabel");
        table.setText(row, 1, (data == null) ? "" : data.toString(), 1, null);
        return row;
    }

    protected static final NumberFormat _fmt = NumberFormat.getFormat("#.##%");
}
