//
// $Id$

package com.threerings.msoy.facebook.server;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.schema.FqlQueryResponse;
import com.threerings.msoy.facebook.server.FQL.Field;

import static com.threerings.msoy.Log.log;

/**
 * Constructs, runs and parses the results of an FQL query.
 */
public class FQLQuery
    implements FQL.Exp
{
    /**
     * A single "row" in an FQL result set.
     */
    public interface Record
    {
        String getField (FQL.Field field);
    }

    /**
     * Creates a new query of some fields in a table with a single clause.
     */
    public FQLQuery (String table, FQL.Field[] fields, FQL.Clause clause)
    {
        _table = table;
        _fields = Arrays.asList(fields);
        _clause = clause;
    }

    /**
     * Runs the query and returns an object to iterate over the resulting records.
     */
    public ResultSet run (FacebookJaxbRestClient client)
        throws FacebookException
    {
        StringBuilder query = new StringBuilder();
        append(query);
        log.info("Running FQL", "string", query);
        return new ResultSet((FqlQueryResponse)client.fql_query(query));
    }

    @Override // from Exp
    public void append (StringBuilder query)
    {
        query.append("select ");
        FQL.join(_fields, query);
        query.append(" from ").append(_table).append(" ");
        _clause.append(query);
    }

    protected int getFieldIdx (FQL.Field field)
    {
        for (int ii = 0, ll = _fields.size(); ii < ll; ++ii) {
            if (_fields.get(ii) == field) {
                return ii;
            }
        }
        throw new NoSuchElementException();
    }

    protected class ResultSet
        implements Iterable<Record>
    {
        public ResultSet (FqlQueryResponse response)
        {
            _results = response.getResults();
        }

        @Override // from Iterable
        public Iterator<Record> iterator ()
        {
            return new Iterator<Record>() {
                public Record next () {
                    return getRecord(_pos++);
                }
                public boolean hasNext () {
                    return _pos < _results.size();
                }
                public void remove () {
                    throw new UnsupportedOperationException();
                }
                protected int _pos;
            };
        }

        protected Record getRecord (int idx)
        {
            final Node node = (Node)_results.get(idx);
            return new Record() {
                @Override public String getField (Field field) {
                    Node fieldNode = node.getChildNodes().item(getFieldIdx(field));
                    Text textContent = (Text)fieldNode.getFirstChild();
                    return textContent.getData();
                }
            };
        }

        protected List<Object> _results;
    }

    protected String _table;
    protected List<FQL.Field> _fields;
    protected FQL.Clause _clause;
}
