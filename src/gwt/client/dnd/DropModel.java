//
// $Id$

package client.dnd;

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

    /**
     * Indicates whether this model allows duplicate entries (whether it is a set of items).
     */
    public boolean allowsDuplicates ();

    /**
     * Adds a listener interested in getting notified when the model contents change.
     */
    public void addDropListener (DropListener<T> listener);

    /**
     * Removes a listener that has grown bored with receiving notifications when the model changes.
     */
    public void removeDropListener (DropListener<T> listener);
}
