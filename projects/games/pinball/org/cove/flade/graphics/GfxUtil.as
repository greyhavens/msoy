/**
 * Flade - Flash Dynamics Engine
 * Release 0.6 alpha 
 * Graphics class
 * Copyright 2004, 2005 Alec Cove
 * 
 * This file is part of Flade. The Flash Dynamics Engine. 
 *	
 * Flade is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Flade is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Flade; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Flash is a registered trademark of Macromedia
 */

package org.cove.flade.graphics {

import flash.display.Graphics;

//TBD: rename this to reflect its vector and/or default nature
public class GfxUtil {

	public static function paintLine (
			gfx:Graphics, 
			x0:Number, 
			y0:Number, 
			x1:Number, 
			y1:Number):void {
		
		gfx.moveTo(x0, y0);
		gfx.lineTo(x1, y1);
	}


	public static function paintCircle (gfx:Graphics, x:Number, y:Number,
                                            r:Number, color:int = -1):void {
            if (color != -1) {
                gfx.beginFill(color);
            }
            gfx.drawCircle(x, y, r);
	}
	
	
	public static function paintRectangle(
			gfx:Graphics, 
			x:Number, 
			y:Number, 
			w:Number, 
			h:Number):void {
            gfx.beginFill(Math.random() * 0xFFFFFF);
            gfx.drawRect(x - w/2, y - h/2, w, h);
	}
}

}
