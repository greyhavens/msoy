//
// $Id$

package com.threerings.msoy.facebook.server;

import java.util.Collection;
import java.util.Iterator;

/**
 * Various classes and methods for constructing statements and parsing the results from the
 * Facebook query language.
 */
public class FQL
{
    /**
     * An expression component of a query, such as "'foo'", "5" or "id in (1,2,3)".
     */
    public interface Exp
    {
        /**
         * Writes this expression into a query in the process of being constructed.
         */
        void append (StringBuilder query);
    }

    /**
     * A clause, such as "where userId = 556549405"
     */
    public interface Clause
    {
        /**
         * Writes this clause into a query in the process of being constructed.
         */
        void append (StringBuilder query);
    }

    /**
     * A where clause.
     */
    public static class Where
        implements Clause
    {
        /**
         * Creates a new where clause specifying the given condition.
         */
        public Where (Exp condition)
        {
            _condition = condition;
        }

        @Override // from Clause
        public void append (StringBuilder query)
        {
            query.append("where ");
            _condition.append(query);
        }

        protected Exp _condition;
    }

    /**
     * Creates a new "in" expression specifying the given expression as being in the set.
     */
    public static Exp in (final Exp exp, final Collection<Exp> set)
    {
        return new Exp() {
            public void append (StringBuilder query) {
                exp.append(query);
                query.append(" in (");
                join(set, query);
                query.append(")");
            }
        };
    }

    /**
     * Represents a "column" in FQL, such as "uid" or "is_app_user".
     */
    public static class Field
        implements Exp
    {
        /** The name of the field. */
        public String name;

        /**
         * Creates a new field with the given name.
         */
        public Field (String name)
        {
            this.name = name;
        }

        @Override // from Exp
        public void append (StringBuilder query)
        {
            query.append(name);
        }

        /**
         * Creates an expression specifying that this field is in a given set.
         */
        public Exp in (final Collection<Exp> set)
        {
            return FQL.in(this, set);
        }
    }

    /**
     * Creates an unquoted literal using the <code>toString</code> of the given value.
     */
    public static Exp unquoted (final Object value)
    {
        return new Exp() {
            public void append (StringBuilder query) {
                query.append(value);
            }
        };
    }

    /**
     * Joins a collection of expressions into a string with a comma between each two consecutive
     * elements.
     */
    public static <T extends Exp> void join (Collection<T> items, StringBuilder output)
    {
        Iterator<T> iter = items.iterator();
        if (iter.hasNext()) {
            iter.next().append(output);
        }
        while (iter.hasNext()) {
            output.append(", ");
            iter.next().append(output);
        }
    }
}
