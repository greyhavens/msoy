//
// $Id$

package client.util;

import java.util.Iterator;

/**
 * Array related utility methods.
 */
public class ArrayUtil
{
    /**
     * Creates an iterator over the supplied array. If the array is null, null is returned.
     */
    public static <T> Iterator<T> toIterator (final T[] data)
    {
        if (data == null) {
            return null;
        }
        return new Iterator<T>() {
            public boolean hasNext () {
                return (_index < data.length);
            }
            public T next () {
                return data[_index++];
            }
            public void remove () {
                throw new RuntimeException("remove() not possible");
            }
            protected int _index;
        };
    }
}
