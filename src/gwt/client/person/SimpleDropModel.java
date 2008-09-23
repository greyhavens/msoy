//
// $Id$

package client.person;

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

    public void insert (T content, int index)
    {
        // check to see if the dropped content is already in the list
        _contents.remove(content);
        index = Math.min(_contents.size(), index);
        _contents.add(index, content);

        // TODO delete this
        // CShell.log("Inserting "+content+" at "+index);
        // CShell.log("Contents "+this);
    }

    public void remove (T content)
    {
        _contents.remove(content);
    }

    public List<T> getContents ()
    {
        return _contents;
    }

    @Override
    public String toString ()
    {
        return _contents.toString();
    }

    protected List<T> _contents;
}
