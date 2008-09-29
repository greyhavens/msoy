//
// $Id$

package client.dnd;

/**
 * Listens for changes to the DropModel.
 *
 * @author mjensen
 */
public interface DropListener<T>
{
    /**
     * Called when content is added to the model.
     */
    public void contentInserted (DropModel<T> model, T content, int index);

    /**
     * Called when content is removed from the model.
     */
    public void contentRemoved (DropModel<T> model, T content);
}
