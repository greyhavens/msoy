//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains a collection of statistics data that can be rendered in the GWT client in a fancy
 * table.
 */
public class StatsModel implements IsSerializable
{
    public static enum Type { DEPOT, DEPOT_QUERIES, CACHE, RPC };

    public static abstract class Cell implements IsSerializable {
        public abstract long value (StatsModel model);

        public void init (int row, int column) {
        }
    }

    public static Cell newCell (long value)
    {
        ValueCell cell = new ValueCell();
        cell._value = value;
        return cell;
    }

    public static Cell newPercentCell (Cell part, Cell remainder)
    {
        PercentCell cell = new PercentCell();
        cell._part = part;
        cell._remainder = remainder;
        return cell;
    }

    public static Cell newAddCell (Cell left, Cell right)
    {
        AddCell cell = new AddCell();
        cell._left = left;
        cell._right = right;
        return cell;
    }

    public static Cell newDivCell (long numer, Cell denom)
    {
        return newDivCell(newCell(numer), denom);
    }

    public static Cell newDivCell (Cell numer, Cell denom)
    {
        DivCell cell = new DivCell();
        cell._numer = numer;
        cell._denom = denom;
        return cell;
    }

    public static Cell newRowSumCell ()
    {
        return new RowSumCell();
    }

    public static Cell newRowAverageCell ()
    {
        return new RowAverageCell();
    }

    public static Cell newColSumCell ()
    {
        return new ColumnSumCell();
    }

    public static Cell newColAverageCell ()
    {
        return new ColumnAverageCell();
    }

    public static StatsModel newColumnModel (String titleHeader, String ... columnHeaders)
    {
        StatsModel model = new StatsModel();
        model._titleHeader = titleHeader;
        model._headers = new ArrayList<String>(columnHeaders.length);
        for (String header : columnHeaders) {
            model._headers.add(header);
        }
        model._rows = new ArrayList<Row>();
        return model;
    }

    public static StatsModel newRowModel (String titleHeader, String ... rowTitles)
    {
        StatsModel model = new StatsModel();
        model._titleHeader = titleHeader;
        model._headers = new ArrayList<String>();
        model._rows = new ArrayList<Row>();
        for (String title : rowTitles) {
            model._rows.add(new Row(4, title));
        }
        return model;
    }

    public String getTitleHeader ()
    {
        return _titleHeader;
    }

    public int getColumns ()
    {
        return _headers.size();
    }

    public int getRows ()
    {
        return _rows.size();
    }

    public long value (int row, int column)
    {
        return getCell(row, column).value(this);
    }

    public String getColumnHeader (int column)
    {
        return _headers.get(column);
    }

    public String getRowTitle (int row)
    {
        return _rows.get(row).title;
    }

    public Cell getCell (int row, int column)
    {
        return _rows.get(row).cells.get(column);
    }

    /**
     * Adds a row to a model been created with {@link #newColumnModel}.
     */
    public void addRow (String title, Cell... cells)
    {
        int cols = getColumns();
        if (cols != 0 && cells.length != cols) {
            throw new IllegalArgumentException("Requested to add row of invalid width " +
                                               "[got=" + cells.length + ", wanted=" + cols + "]");
        }
        Row row = new Row(cells.length, title);
        for (int cc = 0; cc < cells.length; cc++) {
            Cell cell = cells[cc];
            cell.init(_rows.size(), cc);
            row.cells.add(cell);
        }
        _rows.add(row);
    }

    /**
     * Adds a column to a model been created with {@link #newRowModel}.
     */
    public void addColumn (String header, Cell... cells)
    {
        _headers.add(header);
        for (int rr = 0; rr < cells.length; rr++) {
            Row row = _rows.get(rr);
            Cell cell = cells[rr];
            cell.init(rr, row.cells.size());
            row.cells.add(cell);
        }
    }

    protected static class ValueCell extends Cell {
        public long value (StatsModel model) {
            return _value;
        }
        protected long _value;
    }

    protected static class AddCell extends Cell {
        public long value (StatsModel model) {
            return _left.value(model) + _right.value(model);
        }
        protected Cell _left, _right;
    }

    protected static class DivCell extends Cell {
        public long value (StatsModel model) {
            return _numer.value(model) / _denom.value(model);
        }
        protected Cell _numer, _denom;
    }

    protected static class PercentCell extends Cell {
        public long value (StatsModel model) {
            long part = _part.value(model), denom = (part + _remainder.value(model));
            return (denom == 0) ? 0 : 100 * part / denom;
        }
        protected Cell _part, _remainder;
    }

    protected abstract static class FunctionCell extends Cell {
        public void init (int row, int column) {
            _row = row;
            _column = column;
        }
        protected int _row, _column;
    }

    protected static class RowSumCell extends FunctionCell {
        public long value (StatsModel model) {
            long sum = 0;
            for (int cc = 0; cc < _column; cc++) {
                sum += model.getCell(_row, cc).value(model);
            }
            return sum;
        }
    }

    protected static class RowAverageCell extends FunctionCell {
        public long value (StatsModel model) {
            long sum = 0, count = 0;
            for (int cc = 0; cc < _column; cc++) {
                sum += model.getCell(_row, cc).value(model);
                count++;
            }
            return sum / count;
        }
    }

    protected static class ColumnSumCell extends FunctionCell {
        public long value (StatsModel model) {
            long sum = 0;
            for (int rr = 0; rr < _row; rr++) {
                sum += model.getCell(rr, _column).value(model);
            }
            return sum;
        }
    }

    protected static class ColumnAverageCell extends FunctionCell {
        public long value (StatsModel model) {
            long sum = 0, count = 0;
            for (int rr = 0; rr < _row; rr++) {
                sum += model.getCell(rr, _column).value(model);
                count++;
            }
            return sum / count;
        }
    }

    protected static class Row implements IsSerializable
    {
        public String title;
        public List<Cell> cells;

        public Row (int initialCapacity, String title) {
            this.title = title;
            this.cells = new ArrayList<Cell>(initialCapacity);
        }

        public Row () {
            // used for GWT unserialization
        }
    }

    /** The column header for the row title column. */
    protected String _titleHeader;

    /** Our cells, as a list of rows (each of which is a list of columns). */
    protected List<Row> _rows;

    /** Our column headers (including the title header). */
    protected List<String> _headers;
}
