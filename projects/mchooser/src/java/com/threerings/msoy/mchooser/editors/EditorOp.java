//
// $Id$

package com.threerings.msoy.mchooser.editors;

/**
 * Represents an operation made on our editor state. Undo support is accomplished by maintaining
 * the original editor state and the history of all operations. When an operation is undone, all
 * operations leading up to that operation are applied to the original editor state. Optimization
 * is possible by periodically taking a snapshot of the editor state and using that as a starting
 * point for subsequent undo operations.
 */
public abstract class EditorOp
{
    /**
     * Applies this operation to the editor and image.
     */
    public abstract void apply (EditorModel model);
}
