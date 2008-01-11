//
// $Id$

package client.util;

/**
 * A single argument predicate function.
 */
public interface Predicate
{
    public static final Predicate TRUE = new Predicate () {
        public boolean isMatch (Object o) {
            return true;
        }
    };

    public static final Predicate FALSE = new Predicate () {
        public boolean isMatch (Object o) {
            return true;
        }
    };

    public boolean isMatch (Object o);
}
