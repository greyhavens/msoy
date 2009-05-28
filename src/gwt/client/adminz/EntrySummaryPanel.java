//
// $Id$

package client.adminz;

import java.util.Collections;
import java.util.List;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.EntrySummary;

import client.ui.MsoyUI;

/**
 * Displays a summary of Whirled entries.
 */
public class EntrySummaryPanel extends AdminDataPanel<List<EntrySummary>>
{
    public EntrySummaryPanel ()
    {
        super("entrySummary");
        _adminsvc.summarizeEntries(createCallback());
    }

    @Override // from AdminDataPanel
    protected void init (List<EntrySummary> entries)
    {
        if (entries.size() == 0) {
            addNoDataMessage(_msgs.espNoEntries());
            return;
        }
        Collections.sort(entries);

        final SmartTable table = new SmartTable(5, 0);
        table.setWidth("100%");
        int col = 0;
        table.setText(0, col++, _msgs.espVector(), 1, "Header");
        final TextBox filter = MsoyUI.createTextBox("", 32, 16);
        table.setWidget(0, col++, filter);
        table.setText(0, col++, _msgs.espEntries(), 1, "Header");
        table.setText(0, col++, _msgs.espRegistered(), 1, "Header");
        table.setText(0, col++, _msgs.espConversion(), 1, "Header");
        int tentries = 0, tregis = 0;
        for (EntrySummary entry : entries) {
            int row = addRow(table, entry.vector, entry.entries, entry.registrations);
            if (row % 2 == 1) {
                table.getRowFormatter().addStyleName(row, "AltRow");
            }
            tentries += entry.entries;
            tregis += entry.registrations;
        }
        addRow(table, _msgs.espTotal(), tentries, tregis);
        table.getFlexCellFormatter().setStyleName(table.getRowCount(), 0, "Header"); // bold total
        add(table);

        filter.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress (KeyPressEvent event) {
                if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                    String query = filter.getText().trim();
                    int tentries = 0, tregis = 0, lastRow = table.getRowCount()-1, crow = 0;
                    for (int row = 1; row < lastRow; row++) {
                        boolean viz = table.getText(row, 0).contains(query);
                        table.getRowFormatter().setVisible(row, viz);
                        if (viz) {
                            tentries += Integer.parseInt(table.getText(row, 1));
                            tregis += Integer.parseInt(table.getText(row, 2));
                            table.getRowFormatter().removeStyleName(row, "AltRow");
                            if (++crow % 2 == 1) {
                                table.getRowFormatter().addStyleName(row, "AltRow");
                            }
                        }
                    }
                    setRow(table, lastRow, _msgs.espTotal(), tentries, tregis);
                }
            }
        });
    }

    protected int addRow (SmartTable table, String vector, int entries, int registrations)
    {
        int row = table.getRowCount();
        setRow(table, row, vector, entries, registrations);
        return row;
    }

    protected void setRow (SmartTable table, int row, String vector, int entries, int registrations)
    {
        int col = 0;
        table.setText(row, col++, vector, 2, null);
        table.setText(row, col++, ""+entries, 1, "rightLabel");
        table.setText(row, col++, ""+registrations, 1, "rightLabel");
        if (entries > 0) {
            float convert = Math.round(10000*registrations/entries)/100f;
            table.setText(row, col++, _msgs.espConvert(""+convert), 1, "rightLabel");
        }
    }
}
