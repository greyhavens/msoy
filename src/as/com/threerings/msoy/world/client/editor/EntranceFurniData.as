//
// $Id$

package com.threerings.msoy.world.client.editor {

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.world.data.FurniData;

/**
 * This class is a version of furni data used by "fake" entrance furnis. It's only used to
 * differentiate entrances from other furnis; it provides no new functionality. 
 */
public class EntranceFurniData extends FurniData
{
    public function EntranceFurniData ()
    {
        super();
        this.itemType = Item.NOT_A_TYPE;
    }    
}
}
