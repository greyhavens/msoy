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
package com.jswiff.swfrecords.tags;

/**
 * Base class for definition tags, which define new characters.
 */
public abstract class DefinitionTag extends Tag {
	protected int characterId;

	/**
	 * Sets the character ID. Character IDs start at 1 and have to be unique.
	 *
	 * @param characterId character ID
	 */
	public void setCharacterId(int characterId) {
		this.characterId = characterId;
	}

	/**
	 * Returns the character ID. Character IDs start at 1 and have to be
	 * unique.
	 *
	 * @return character ID
	 */
	public int getCharacterId() {
		return characterId;
	}
}
