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
	
	/**
	 * @private
	 * 
	 * This has to be implemented with the following:
	 * - correctly models mass
	 * - correctly models leverage
	 * - is stable without a lot of damping
	 * - is stable when connected to any other (reasonable) configuration
	 */
	public class AngularConstraint extends AbstractConstraint {

		public function AngularConstraint(
				p1:AbstractParticle, 
				p2:AbstractParticle, 
				p3:AbstractParticle, 
				stiffness:Number = 0.4) {
		}

	}
}


