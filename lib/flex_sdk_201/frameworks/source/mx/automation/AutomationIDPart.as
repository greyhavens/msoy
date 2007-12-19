////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation
{

/**
 *  The AutomationIDPart class represents a component instance to agents.
 */
public dynamic class AutomationIDPart
{
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    public function AutomationIDPart()
    {
        super();
    }

    //--------------------------------------------------------------------------
    //
    //  Methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    public function toString():String
    {
        var result:String = "";
        
        for (var p:String in this)
        {
            result += encodeURI(p) + "{" +
                      encodeURI(this[p]) + " " +
                      typeof(this[p]) + "}";
        }
        
        return result.toString();
    }
}

}
