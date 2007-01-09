/*
APE (Actionscript Physics Engine) is an AS3 open source 2D physics engine
Copyright 2006, Alec Cove 

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

Contact: ape@cove.org
*/
package org.cove.ape {
	
	internal class RimParticle {
		
		internal var curr:Vector;
		internal var prev:Vector;
	
		private var wr:Number;
		private var av:Number;
		private var sp:Number;
		private var maxTorque:Number;
		
		
		/**
		 * The RimParticle is really just a second component of the wheel model.
		 * The rim particle is simulated in a coordsystem relative to the wheel's 
		 * center, not in worldspace.
		 * 
		 * Origins of this code are from Raigan Burns, Metanet Software
		 */
		public function RimParticle(r:Number, mt:Number) {
	
			curr = new Vector(r, 0);
			prev = new Vector(0, 0);
			
			sp = 0; 
			av = 0;
			
			maxTorque = mt; 	
			wr = r;		
		}
		
		internal function get speed():Number {
			return sp;
		}
		
		internal function set speed(s:Number):void {
			sp = s;
		}
		
		internal function get angularVelocity():Number {
			return av;
		}
		
		internal function set angularVelocity(s:Number):void {
			av = s;
		}
		
		/**
		 * Origins of this code are from Raigan Burns, Metanet Software
		 */
		internal function update(dt:Number):void {
			
			// USE VECTOR METHODS HERE		
			
			//clamp torques to valid range
			sp = Math.max(-maxTorque, Math.min(maxTorque, sp + av));
	
			//apply torque
			//this is the tangent vector at the rim particle
			var dx:Number = -curr.y;
			var dy:Number =  curr.x;
	
			//normalize so we can scale by the rotational speed
			var len:Number = Math.sqrt(dx * dx + dy * dy);
			dx /= len;
			dy /= len;
	
			curr.x += sp * dx;
			curr.y += sp * dy;		
	
			var ox:Number = prev.x;
			var oy:Number = prev.y;
			var px:Number = prev.x = curr.x;		
			var py:Number = prev.y = curr.y;		
			
			curr.x += APEngine.damping * (px - ox);
			curr.y += APEngine.damping * (py - oy);	
	
			// hold the rim particle in place
			var clen:Number = Math.sqrt(curr.x * curr.x + curr.y * curr.y);
			var diff:Number = (clen - wr) / clen;
	
			curr.x -= curr.x * diff;
			curr.y -= curr.y * diff;
		}
	}
}



