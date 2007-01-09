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
	
	internal class SpringConstraintParticle extends RectangleParticle {
		
		private var p1:AbstractParticle;
		private var p2:AbstractParticle;
		private var avgVelocity:Vector;
		
		
		public function SpringConstraintParticle(p1:AbstractParticle, p2:AbstractParticle) {
			super(0,0,0,0,0,false);
			this.p1 = p1;
			this.p2 = p2;
			avgVelocity = new Vector(0,0);
		}
		
		
		/**
		 * returns the average mass of the two connected particles
		 */
		public override function get mass():Number {
			return (p1.mass + p2.mass) / 2; 
		}
		
		
		/**
		 * returns the average velocity of the two connected particles
		 */
		public override function get velocity():Vector {
			var p1v:Vector =  p1.velocity;
			var p2v:Vector =  p2.velocity;
			
			avgVelocity.setTo(((p1v.x + p2v.x) / 2), ((p1v.y + p2v.y) / 2));
			return avgVelocity;
		}	
		
		
		public override function paint():void {
			if (_cornerPositions != null) updateCornerPositions();
			super.paint();
		}
		
		
		internal override function resolveCollision(mtd:Vector, vel:Vector, n:Vector, d:Number, o:Number):void {
		
			if (! p1.fixed) {
				p1.curr.plusEquals(mtd);
				p1.velocity = vel;	
			}
			
			if (! p2.fixed) {
				p2.curr.plusEquals(mtd);
				p2.velocity = vel;	
			}
		}
	}
}