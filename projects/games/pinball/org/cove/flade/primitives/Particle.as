/**
 * Flade - Flash Dynamics Engine
 * Release 0.6 alpha 
 * Particle class
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

package org.cove.flade.primitives {

import org.cove.flade.util.*;
import org.cove.flade.surfaces.*;
import org.cove.flade.DynamicsEngine;

import flash.display.Sprite;


public class Particle extends Sprite {
    public var h:Number;
    public var w:Number;
	public var curr:Vector;
	public var prev:Vector;
	public var bmin:Number;
	public var bmax:Number;
	public var mtd:Vector;

	protected var extents:Vector;

	protected var init:Vector;
	protected var temp:Vector;
	
	protected var isVisible:Boolean;
	

	public function Particle(posX:Number, posY:Number) {
		
		// store initial position, for pinning
		init = new Vector(posX, posY);
		
		// current and previous positions - for integration
		curr = new Vector(posX, posY);
		prev = new Vector(posX, posY);
		temp = new Vector(0,0);
		
		// attributes for collision detection with tiles
		this.extents = new Vector(0, 0); 

		bmin = 0;
		bmax = 0;
		mtd = new Vector(0,0);
		
		isVisible = true;
	}

	public function setVisible(v:Boolean):void {
		isVisible = v;
	}

	
	public function verlet(sysObj:DynamicsEngine):void {
		
		temp.x = curr.x;
		temp.y = curr.y;
		
		curr.x += sysObj.coeffDamp * (curr.x - prev.x) + sysObj.gravity.x;
		curr.y += sysObj.coeffDamp * (curr.y - prev.y) + sysObj.gravity.y;

		prev.x = temp.x;
		prev.y = temp.y;
	}
	
	
	public function pin():void {
		curr.x = init.x;
		curr.y = init.y;
		prev.x = init.x;
		prev.y = init.y;
	}
	
	
	public function setPos(px:Number, py:Number):void {
		curr.x = px;
		curr.y = py;
		prev.x = px;
		prev.y = py;
	}


	/**
	 * Get projection onto a cardinal (world) axis x 
	 */
	// TBD: rename to something other than "get" 
	// TBD: there is another implementation of this in the 
	// AbstractTile base class.
	public function getCardXProjection():void {
		bmin = curr.x - extents.x;
		bmax = curr.x + extents.x;
	}


	/**
	 * Get projection onto a cardinal (world) axis y
	 */	
	// TBD: there is another implementation of this in the 
	// AbstractTile base class. see if they can be combined
	public function getCardYProjection():void {
		bmin = curr.y - extents.y;
		bmax = curr.y + extents.y;
	}


	/**
	 * Get projection onto arbitrary axis. Note that axis need not be unit-length. If
	 * it is not, min and max will be scaled by the length of the axis. This is fine
	 * if all we're doing is comparing relative values. If we need the 'actual' projection,
	 * the axis should be unit length.
	 */
	public function getAxisProjection(axis:Vector):void {
		var absAxis:Vector = new Vector(Math.abs(axis.x), Math.abs(axis.y));
		var projectedCenter:Number = curr.dot(axis);
		var projectedRadius:Number = extents.dot(absAxis);

		bmin = projectedCenter - projectedRadius;
		bmax = projectedCenter + projectedRadius;
	}


	/**
	 * Find minimum depth and set mtd appropriately. mtd is the minimum translational 
	 * distance, the vector along which we must move the box to resolve the collision.
	 */
	 //TBD: this is only for right triangle surfaces - make generic
	public function setMTD(depthX:Number, depthY:Number, depthN:Number, surfNormal:Vector):void {

		var absX:Number = Math.abs(depthX);
		var absY:Number = Math.abs(depthY);
		var absN:Number = Math.abs(depthN);

		if (absX < absY && absX < absN) {
			mtd.setTo(depthX, 0);
		} else if (absY < absX && absY < absN) {
			mtd.setTo(0, depthY);
		} else if (absN < absX && absN < absY) {
			mtd = surfNormal.multNew(depthN);
		}
	}


	/**
	 * Set the mtd for situations where there are only the x and y axes to consider.
	 */
	public function setXYMTD(depthX:Number, depthY:Number):void {

		var absX:Number = Math.abs(depthX);
		var absY:Number = Math.abs(depthY);

		if (absX < absY) {
			mtd.setTo(depthX, 0);
		} else {
			mtd.setTo(0, depthY);
		}
	}
	
	
	// TBD: too much passing around of the DynamicsEngine object. Probably better if
	// it was static.  there is no way to individually set the kfr and friction of the
	// surfaces since they are calculated here from properties of the DynamicsEngine
	// object. Also, review for too much object creation
	public function resolveCollision(normal:Vector, sysObj:DynamicsEngine):void {
		// get the velocity
		var vel:Vector = curr.minusNew(prev);
		var sDotV:Number = normal.dot(vel);

		// compute momentum of particle perpendicular to normal
		var velProjection:Vector = vel.minusNew(normal.multNew(sDotV));
		var perpMomentum:Vector = velProjection.multNew(sysObj.coeffFric);

		// compute momentum of particle in direction of normal
		var normMomentum:Vector = normal.multNew(sDotV * sysObj.coeffRest - sysObj.coeffPinball);
		var totalMomentum:Vector = normMomentum.plusNew(perpMomentum);

		// set new velocity w/ total momentum
		var newVel:Vector = vel.minusNew(totalMomentum);

		// project out of collision
		curr.plus(mtd);

		// apply new velocity
		prev = curr.minusNew(newVel);		
	}
	

	public function paint():void {
	}


	public function checkCollision(surface:Surface, sysObj:DynamicsEngine):void {
	}


}


}
