/**
 * Flade - Flash Dynamics Engine
 * Release 0.6 alpha 
 * Surface interface
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

package org.cove.flade.surfaces {
 
import org.cove.flade.primitives.*;
import org.cove.flade.DynamicsEngine;

public interface Surface {
	
	function paint():void

	function getActiveState():Boolean
	function setActiveState(s:Boolean):void
	
	function resolveCircleCollision(p:CircleParticle, sysObj:DynamicsEngine):void
	function resolveRectangleCollision(p:RectangleParticle, sysObj:DynamicsEngine):void
}
}