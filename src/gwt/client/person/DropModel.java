//
// $Id$

package client.person;

import java.util.List;

/**
 * The interface through which the DropPanel accesses its data.
 *
 * @param T the content type.
 * @author mjensen
 */
public interface DropModel<T>
{
    /**
     * Called when an item is dropped into this model.
     */
    public void insert (T content, int index);

    /**
     * Called when an item is removed from this model.
     */
    public void remove (T content);

    /**
     * Gets the current contents of the model.
     */
    public List<T> getContents ();

    // TODO void addDropListener (DropListener listener);
    // TODO void removeDropListener (DropListener listener);
}
