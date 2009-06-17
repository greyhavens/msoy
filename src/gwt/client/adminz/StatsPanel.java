//
// $Id$

package client.adminz;

import java.util.Arrays;
import java.util.Comparator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.ListBox;

import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.SortableGrid;
import com.google.gwt.gen2.table.client.TableModelHelper.ColumnSortList;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.StatsModel;

import client.ui.MsoyUI;
import client.util.InfoCallback;

/**
 * Displays server statistics.
 */
public class StatsPanel extends FlowPanel
{
    public StatsPanel ()
    {
        addStyleName("statsPanel");

        _controls = new FlowPanel();
        _controls.setStyleName("Controls");
        _controls.add(MsoyUI.createLabel(_msgs.statsType(), "inline"));
        _controls.add(_types = new ListBox());
        for (StatsModel.Type type : StatsModel.Type.values()) {
            _types.addItem(type.toString());
        }
        _types.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent sender) {
                refresh();
            }
        });
        _controls.add(new Button(_msgs.statsRefresh(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                refresh();
            }
        }));
        add(_controls);
    }

    protected void refresh ()
    {
        String tstr = _types.getItemText(_types.getSelectedIndex());
        final StatsModel.Type type = Enum.valueOf(StatsModel.Type.class, tstr);
        _adminsvc.getStatsModel(type, new InfoCallback<StatsModel>() {
            public void onSuccess (StatsModel model) {
                displayReport(type, model);
            }
        });
    }

    protected void displayReport (StatsModel.Type type, StatsModel model)
    {
        clear();
        add(_controls);

        // set up the header
        FixedWidthFlexTable header = new FixedWidthFlexTable();
        header.setCellSpacing(0);
        header.setText(0, 0, model.getTitleHeader());
        for (int cc = 0, lc = model.getColumns(); cc < lc; cc++) {
            header.setText(0, cc+1, model.getColumnHeader(cc));
            header.getFlexCellFormatter().setHorizontalAlignment(
                0, cc+1, HasAlignment.ALIGN_CENTER);
        }

        // set up the table contents
        FixedWidthGrid data = new FixedWidthGrid();
        data.setCellSpacing(0);
        data.resize(model.getRows(), model.getColumns() + 1);
//         data.setColumnSorter(new StatsColumnSorter(model));
        for (int rr = 0, lr = model.getRows(); rr < lr; rr++) {
            data.setText(rr, 0, model.getRowTitle(rr));
            for (int cc = 0, lc = model.getColumns(); cc < lc; cc++) {
                data.setText(rr, cc+1, ""+model.getCell(rr, cc).value(model));
                data.getCellFormatter().setHorizontalAlignment(rr, cc+1, HasAlignment.ALIGN_RIGHT);
            }
        }

        // create our table
        ScrollTable table = new ScrollTable(data, header);
        table.setResizePolicy(ScrollTable.ResizePolicy.UNCONSTRAINED);
        table.setScrollPolicy(ScrollTable.ScrollPolicy.HORIZONTAL);
        table.setSize("100%", "100%");
        int availwid = 700/*todo*/ - 11 - (model.getColumns()+1)*3; // border, colgap
        int colwid = Math.min(100, Math.max(DATA_WIDTH, (availwid-200)/model.getColumns()));
        table.setColumnWidth(0, Math.max(100, availwid - (model.getColumns() * colwid)));
        for (int cc = 0; cc < model.getColumns(); cc++) {
            table.setColumnWidth(cc+1, colwid);
        }
        add(table);
    }

    protected static class StatsColumnSorter extends SortableGrid.ColumnSorter
    {
        public StatsColumnSorter (StatsModel model) {
            _model = model;
            _curIndexes = makeIndexes();
        }

        @Override // from SortableGrid.ColumnSorter
        public void onSortColumn(SortableGrid grid,
            ColumnSortList sortList, SortableGrid.ColumnSorterCallback callback) {
            final int column = sortList.getPrimaryColumn()-1; // -1 accounts for row label
            final boolean ascending = sortList.isPrimaryAscending();
            Integer[] newIndexes = makeIndexes();
            Arrays.sort(newIndexes, new Comparator<Integer>() {
                public int compare (Integer r1, Integer r2) {
                    long r1v = _model.value(_curIndexes[r1], column);
                    long r2v = _model.value(_curIndexes[r2], column);
                    if (ascending) {
                        return (r1v > r2v) ? 1 : (r1v < r2v ? -1 : 0);
                    } else {
                        return (r2v > r1v) ? 1 : (r2v < r1v ? -1 : 0);
                    }
                }
            });
            int[] indexes = new int[newIndexes.length];
            for (int ii = 0; ii < _curIndexes.length; ii++) {
                indexes[ii] = newIndexes[ii];
                _curIndexes[ii] = _curIndexes[newIndexes[ii]];
            }
            callback.onSortingComplete(indexes);
        }

        protected Integer[] makeIndexes () {
            Integer[] indexes = new Integer[_model.getRows()];
            for (int ii = 0; ii < indexes.length; ii++) {
                indexes[ii] = ii;
            }
            return indexes;
        }

        protected StatsModel _model;
        protected Integer[] _curIndexes;
    }

    protected FlowPanel _controls;
    protected ListBox _types;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);

    protected static final int DATA_WIDTH = 50;
}
