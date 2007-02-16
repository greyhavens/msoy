/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2005 Ralf Terdic (contact@jswiff.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.jswiff.swfrecords;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;
import java.io.Serializable;


/**
 * This record defines a simple transform that can be applied to the color
 * space of an object.
 */
public final class CXform implements Serializable {
	private int redMultTerm		 = 256;
	private int greenMultTerm    = 256;
	private int blueMultTerm     = 256;
	private int redAddTerm		 = 0;
	private int greenAddTerm     = 0;
	private int blueAddTerm		 = 0;
	private boolean hasAddTerms;
	private boolean hasMultTerms;

	/**
	 * Creates a new CXform instance. After creation, use setter methods to set
	 * the values of the transform terms.
	 */
	public CXform() {
		// don't do anything
	}

	/**
	 * Reads a CXform instance from a bit stream.
	 *
	 * @param stream the source bit stream
	 *
	 * @throws IOException if an I/O error has occured
	 */
	public CXform(InputBitStream stream) throws IOException {
		hasAddTerms		 = stream.readBooleanBit();
		hasMultTerms     = stream.readBooleanBit();
		int nBits		 = (int) stream.readUnsignedBits(4);
		if (hasMultTerms) {
			redMultTerm		  = (int) stream.readSignedBits(nBits);
			greenMultTerm     = (int) stream.readSignedBits(nBits);
			blueMultTerm	  = (int) stream.readSignedBits(nBits);
		}
		if (hasAddTerms) {
			redAddTerm		 = (int) stream.readSignedBits(nBits);
			greenAddTerm     = (int) stream.readSignedBits(nBits);
			blueAddTerm		 = (int) stream.readSignedBits(nBits);
		}
		stream.align();
	}

	/**
	 * Sets the transform's additive terms. After addition, the result is
	 * truncated at 255 if greater than 255 and at 0 if negative.
	 *
	 * @param redAddTerm red term
	 * @param greenAddTerm green term
	 * @param blueAddTerm blue term
	 */
	public void setAddTerms(int redAddTerm, int greenAddTerm, int blueAddTerm) {
		this.redAddTerm		  = redAddTerm;
		this.greenAddTerm     = greenAddTerm;
		this.blueAddTerm	  = blueAddTerm;
		hasAddTerms			  = true;
	}

	/**
	 * Returns the additive term for the blue component. Check with
	 * <code>hasAddTerms()</code> first if additive terms have been specified.
	 *
	 * @return additive blue term
	 */
	public int getBlueAddTerm() {
		return blueAddTerm;
	}

	/**
	 * Returns the multiplicative term for the blue component. Check with
	 * <code>hasMultTerms()</code> first if multiplicative terms have been
	 * specified.
	 *
	 * @return multiplicative blue term
	 */
	public int getBlueMultTerm() {
		return blueMultTerm;
	}

	/**
	 * Returns the additive term for the green component. Check with
	 * <code>hasAddTerms()</code> first if additive terms have been specified.
	 *
	 * @return additive green term
	 */
	public int getGreenAddTerm() {
		return greenAddTerm;
	}

	/**
	 * Returns the multiplicative term for the green component. Check with
	 * <code>hasMultTerms()</code> first if multiplicative terms have been
	 * specified.
	 *
	 * @return multiplicative green term
	 */
	public int getGreenMultTerm() {
		return greenMultTerm;
	}

	/**
	 * Sets the transform's multiplicative terms. The terms are 8.8 fixed point
	 * values (i.e. 256 corresponds to 1.0, after multiplication the result is
	 * divided by 256).
	 *
	 * @param redMultTerm red term
	 * @param greenMultTerm green term
	 * @param blueMultTerm blue term
	 */
	public void setMultTerms(
		int redMultTerm, int greenMultTerm, int blueMultTerm) {
		this.redMultTerm	   = redMultTerm;
		this.greenMultTerm     = greenMultTerm;
		this.blueMultTerm	   = blueMultTerm;
		hasMultTerms		   = true;
	}

	/**
	 * Returns the additive term for the red component. Check with
	 * <code>hasAddTerms()</code> if additive terms have been specified.
	 *
	 * @return additive red term
	 */
	public int getRedAddTerm() {
		return redAddTerm;
	}

	/**
	 * Returns the multiplicative term for the red component. Check with
	 * <code>hasMultTerms()</code> if multiplicative terms have been
	 * specified.
	 *
	 * @return multiplicative red term
	 */
	public int getRedMultTerm() {
		return redMultTerm;
	}

	/**
	 * Checks if the transform has additive terms.
	 *
	 * @return <code>true</code> if additive terms contained
	 */
	public boolean hasAddTerms() {
		return hasAddTerms;
	}

	/**
	 * Checks if the transform has multiplicative terms.
	 *
	 * @return <code>true</code> if multiplicative terms contained
	 */
	public boolean hasMultTerms() {
		return hasMultTerms;
	}

	/**
	 * Writes the transform to a bit stream.
	 *
	 * @param stream the target bit stream
	 *
	 * @throws IOException if an I/O error has occured
	 */
	public void write(OutputBitStream stream) throws IOException {
		stream.writeBooleanBit(hasAddTerms);
		stream.writeBooleanBit(hasMultTerms);
		int nBits = 0;
		if (hasAddTerms) {
			nBits     = Math.max(
					nBits, OutputBitStream.getSignedBitsLength(redAddTerm));
			nBits     = Math.max(
					nBits, OutputBitStream.getSignedBitsLength(greenAddTerm));
			nBits     = Math.max(
					nBits, OutputBitStream.getSignedBitsLength(blueAddTerm));
		}
		if (hasMultTerms) {
			nBits     = Math.max(
					nBits, OutputBitStream.getSignedBitsLength(redMultTerm));
			nBits     = Math.max(
					nBits, OutputBitStream.getSignedBitsLength(greenMultTerm));
			nBits     = Math.max(
					nBits, OutputBitStream.getSignedBitsLength(blueMultTerm));
		}
		stream.writeUnsignedBits(nBits, 4);
		if (hasMultTerms) {
			stream.writeSignedBits(redMultTerm, nBits);
			stream.writeSignedBits(greenMultTerm, nBits);
			stream.writeSignedBits(blueMultTerm, nBits);
		}
		if (hasAddTerms) {
			stream.writeSignedBits(redAddTerm, nBits);
			stream.writeSignedBits(greenAddTerm, nBits);
			stream.writeSignedBits(blueAddTerm, nBits);
		}
		stream.align();
	}
}
