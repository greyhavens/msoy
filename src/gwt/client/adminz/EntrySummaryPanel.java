//
// $Id$

package client.adminz;

import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.EntrySummary;

import client.ui.MsoyUI;
import client.util.PageCallback;
import client.util.ServiceUtil;

/**
 * Displays a summary of Whirled entries.
 */
public class EntrySummaryPanel extends FlowPanel
{
    public EntrySummaryPanel ()
    {
        setStyleName("entrySummary");

        add(MsoyUI.createNowLoading());

        _adminsvc.summarizeEntries(new PageCallback<List<EntrySummary>>(this) {
            public void onSuccess (List<EntrySummary> data) {
                init(data);
            }
        });
    }

    protected void init (List<EntrySummary> entries)
    {
        clear();
        if (entries.size() == 0) {
            add(MsoyUI.createLabel(_msgs.espNoEntries(), "infoLabel"));
            return;
        }
        Collections.sort(entries);

        SmartTable table = new SmartTable(5, 0);
        table.setText(0, 0, _msgs.espVector(), 1, "Header");
        table.setText(0, 1, _msgs.espEntries(), 1, "Header");
        table.setText(0, 2, _msgs.espRegistered(), 1, "Header");
        table.setText(0, 3, _msgs.espConversion(), 1, "Header");
        for (EntrySummary entry : entries) {
            int row = table.addText(entry.vector, 1, null);
            if (row % 2 == 1) {
                table.getRowFormatter().addStyleName(row, "AltRow");
            }
            table.setText(row, 1, ""+entry.entries, 1, "rightLabel");
            table.setText(row, 2, ""+entry.registrations, 1, "rightLabel");
            if (entry.entries > 0) {
                float convert = Math.round(10000*entry.registrations/entry.entries)/100f;
                table.setText(row, 3, _msgs.espConvert(""+convert), 1, "rightLabel");
            }
        }
        add(table);
    }

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
