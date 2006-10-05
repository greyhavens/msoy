/**
 * Flade - Flash Dynamics Engine
 * Release 0.6 alpha 
 * SpringConstraint class
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
 
package org.cove.flade.constraints {

import org.cove.flade.util.*;
import org.cove.flade.graphics.*;
import org.cove.flade.primitives.*;
import org.cove.flade.constraints.*;

import flash.display.*;

public class SpringConstraint extends Sprite implements Constraint{
	
	protected var p1:Particle;
	protected var p2:Particle;
	protected var restLength:Number;
	protected var tearLength:Number;
	
	protected var color:Number;
	protected var stiffness:Number;
	protected var isVisible:Boolean;

        public function SpringConstraint(p1:Particle, p2:Particle,
                                         v:Boolean = true,
                                         s:Number = 0.5)
        {
		this.p1 = p1;
		this.p2 = p2;
		restLength = p1.curr.distance(p2.curr);

		stiffness = s;
		color = 0x996633;
		
		isVisible = v;
	}
	
	
	public function resolve():void {

		var delta:Vector = p1.curr.minusNew(p2.curr);
		var deltaLength:Number = p1.curr.distance(p2.curr);

		var diff:Number = (deltaLength - restLength) / deltaLength;
		var dmd:Vector = delta.mult(diff * stiffness);

		p1.curr.minus(dmd);
		p2.curr.plus(dmd);
	}


	public function setRestLength(r:Number):void {
		restLength = r;
	}


	public function setStiffness(s:Number):void {
		stiffness = s;
	}


	public function setVisible(v:Boolean):void {
		isVisible = v;
	}


	public function paint():void {
		if (isVisible) {
			graphics.clear();
			graphics.lineStyle(3, color, 1.0,
                                           true, LineScaleMode.NONE,
                                           CapsStyle.ROUND);

			GfxUtil.paintLine(
					graphics,
					p1.curr.x, 
					p1.curr.y, 
					p2.curr.x, 
					p2.curr.y);
		}
	}
}
}
