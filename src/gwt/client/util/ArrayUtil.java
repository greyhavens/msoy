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
     * Workaround for gwt's lack of reflection. In particular we cannot call newInstance or
     * getComponentType.
     */
    public interface ArrayType <T>
    {
        T[] makeNew (int size); 
    }

    /** A string array type. */
    public static final ArrayType<String> STRING_TYPE = new ArrayType<String>() {
        public String[] makeNew (int size) {
            return new String[size];
        }
    };

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

    /**
     * Returns a copy of an array with a range of elements removed.
     * @param <T> type of objects in the array
     * @param values the array to copy
     * @param offset the offset of the first element to remove
     * @param length the number of elements to remove
     * @return the copied array
     */
    public static <T extends Object> T[] splice (
        T[] values, int offset, int length, ArrayType<T> type)
    {
        // make sure we've something to work with
        if (values == null) {
            throw new IllegalArgumentException("Can't splice a null array.");

        } else if (length == 0) {
            // we're not splicing anything!
            return values;
        }

        // require that the entire range to remove be within the array bounds
        int size = values.length;
        int tstart = offset + length;
        if (offset < 0 || tstart > size) {
            throw new ArrayIndexOutOfBoundsException(
                "Splice range out of bounds [offset=" + offset +
                ", length=" + length + ", size=" + size + "].");
        }

        // create a new array and populate it with the spliced-in values
        T[] nvalues = type.makeNew(size - length);
        System.arraycopy(values, 0, nvalues, 0, offset);
        System.arraycopy(values, tstart, nvalues, offset, size - tstart);
        return nvalues;
    }

    /**
     * Returns a copy of an array with a new item inserted at a given offset.
     * @param <T> type of objects in the array
     * @param values the array to copy
     * @param value the value to insert
     * @param index the index to insert at
     * @return the copied array with one additional element
     */
    public static <T extends Object> T[] insert (T[] values, T value, int index, ArrayType<T> type)
    {
        T[] nvalues = type.makeNew(values.length+1);
        if (index > 0) {
            System.arraycopy(values, 0, nvalues, 0, index);
        }
        nvalues[index] = value;
        if (index < values.length) {
            System.arraycopy(values, index, nvalues, index+1, values.length-index);
        }
        return nvalues;
    }

    /**
     * Returns a copy of an array with a new item appended to the end.
     * @param <T> the type of objects in the array
     * @param values the array to copy
     * @param value the value to append
     * @return the copies array with the new element on the end
     */
    public static <T extends Object> T[] append (T[] values, T value, ArrayType<T> type)
    {
        return insert(values, value, values.length, type);
    }
}
