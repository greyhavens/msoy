//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTree;

import org.jvnet.substance.SubstanceDefaultTreeCellRenderer;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.PathElementTreeNode;

/**
 * Overrides DefaultTreeCellRenderer to render a Swiftly specific JTree.
 */
public class ProjectTreeCellRenderer extends SubstanceDefaultTreeCellRenderer
{
    /**
     * Sets the tree icons based on PathElement type.
     */
    @Override // from DefaultTreeCellRenderer
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selection, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, value, selection, expanded,
                                           leaf, row, hasFocus);

        PathElementTreeNode node = (PathElementTreeNode)value;

        setIconAndToolTip(node.getElement());

        return this;
    }

    /**
     * Sets the icon for the node based on the PathElement.
     */
    private void setIconAndToolTip(PathElement element)
    {
        if (element.getType() == PathElement.Type.ROOT) {
            // TODO if we want tool tips here is how. Maybe "This file is being externally edited"
            // setToolTipText("Project Root");
            setFont(getFont().deriveFont(Font.BOLD));
            // no icon for the project root
            setIcon(null);

        } else if (element.getType() == PathElement.Type.FILE) {
            setFont(getFont().deriveFont(Font.PLAIN));
            setIcon(element.getIcon());
        }
    }
}
