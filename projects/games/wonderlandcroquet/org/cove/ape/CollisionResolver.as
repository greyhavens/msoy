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
	
	// NEED TO EXCLUDE VELOCITY CALCS BASED ON collisionResponseMode
	internal final class CollisionResolver {
		
		internal static function resolveParticleParticle(
				pa:AbstractParticle, 
				pb:AbstractParticle, 
				normal:Vector, 
				depth:Number):void {
			
			var mtd:Vector = normal.mult(depth);
			var te:Number = pa.elasticity + pb.elasticity;
			
			// the total friction in a collision is combined but clamped to [0,1]
			var tf:Number = 1 - (pa.friction + pb.friction);
			if (tf > 1) tf = 1;
			else if (tf < 0) tf = 0;
		
			// get the total mass, and assign giant mass to fixed particles
			var ma:Number = (pa.fixed) ? 100000 : pa.mass;
			var mb:Number = (pb.fixed) ? 100000 : pb.mass;
			var tm:Number = ma + mb;
			
			// get the collision components, vn and vt
			var ca:Collision = pa.getComponents(normal);
			var cb:Collision = pb.getComponents(normal);
		 
		 	// calculate the coefficient of restitution based on the mass
			var vnA:Vector = (cb.vn.mult((te + 1) * mb).plus(ca.vn.mult(ma - te * mb))).divEquals(tm);		
			var vnB:Vector = (ca.vn.mult((te + 1) * ma).plus(cb.vn.mult(mb - te * ma))).divEquals(tm);
			ca.vt.multEquals(tf);
			cb.vt.multEquals(tf);
			
			// scale the mtd by the ratio of the masses. heavier particles move less
			var mtdA:Vector = mtd.mult( mb / tm);
			var mtdB:Vector = mtd.mult(-ma / tm);
			
			if (! pa.fixed) pa.resolveCollision(mtdA, vnA.plusEquals(ca.vt), normal, depth, -1);
			if (! pb.fixed) pb.resolveCollision(mtdB, vnB.plusEquals(cb.vt), normal, depth,  1);
		}
	}
}

