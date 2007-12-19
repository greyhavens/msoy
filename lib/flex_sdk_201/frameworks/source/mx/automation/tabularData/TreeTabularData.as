////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation.tabularData
{

import mx.automation.AutomationManager;
import mx.automation.IAutomationObject;
import mx.automation.IAutomationTabularData;
import mx.collections.CursorBookmark;
import mx.collections.errors.ItemPendingError;
import mx.controls.listClasses.IListItemRenderer;
import mx.controls.Tree;
import mx.core.mx_internal;
use namespace mx_internal;

/**
 *  @private
 */
public class TreeTabularData extends ListTabularData
{

    private var tree:Tree;

    /**
     *  Constructor
     */
    public function TreeTabularData(l:Tree)
    {
		super(l);

        tree = l;
    }

    /**
     *  @inheritDoc
     */
    override public function get numRows():int
    {
        return tree.collectionLength;
    }

}
}
