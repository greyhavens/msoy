//
// $Id$

package client.person;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple DropModel implementation that keeps an ordered set of the contents.
 *
 * @author mjensen
 */
public class SimpleDropModel<T> implements DropModel<T>
{
    /**
     * Uses the given list as the backing for this model. Note that the given list <i>will</i> get
     * modified as changes are made to this model.
     */
    public SimpleDropModel (List<T> contents)
    {
        _contents = contents;
    }

    // from DropModel
    public void insert (T content, int index)
    {
        // check to see if the dropped content is already in the list
        _contents.remove(content);
        index = Math.min(_contents.size(), index);
        _contents.add(index, content);
        fireContentInserted(content, index);
    }

    // from DropModel
    public void remove (T content)
    {
        _contents.remove(content);
        fireContentRemoved(content);
    }

    // from DropModel
    public List<T> getContents ()
    {
        return _contents;
    }

    // from DropModel
    public void addDropListener (DropListener<T> listener)
    {
        _listeners.add(listener);
    }

    // from DropModel
    public void removeDropListener (DropListener<T> listener)
    {
        _listeners.remove(listener);
    }

    // from DropModel
    public boolean allowsDuplicates ()
    {
        return _allowsDuplicates;
    }

    @Override
    public String toString ()
    {
        return _contents.toString();
    }

    protected void fireContentInserted (T content, int index)
    {
        for (DropListener<T> listener : _listeners) {
            listener.contentInserted(this, content, index);
        }
    }

    protected void fireContentRemoved (T content)
    {
        for (DropListener<T> listener : _listeners) {
            listener.contentRemoved(this, content);
        }
    }

    protected List<T> _contents;
    protected List<DropListener<T>> _listeners = new ArrayList<DropListener<T>>();
    protected boolean _allowsDuplicates;
}
