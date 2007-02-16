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

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;
import java.io.Serializable;


/**
 * <p>
 * This tag imports one or more characters from an SWF file. The imported
 * characters must have been previously exported with an
 * <code>ExportAssets</code> tag.
 * </p>
 * 
 * <p>
 * The character IDs of the imported characters presumably differ from the IDs
 * in the exporting file, therefore the characters chosen for export are
 * identified by (unique) export names. The character IDs are mapped to names
 * within a <code>ExportAssets</code> tag (using <code>ExportMapping</code>
 * instances). After import, these names are mapped back to (different)
 * character IDs within <code>ImportAssets</code> (using
 * <code>ImportMapping</code> instances).
 * </p>
 *
 * @see ExportAssets
 * @since SWF 5
 */
public class ImportAssets extends Tag {
	protected String url;
	protected ImportMapping[] importMappings;

	/**
	 * Creates a new ImportAssets tag. Supply the URL of the exporting SWF and
	 * an array of import mappings (for each imported character one).
	 *
	 * @param url URL of the source SWF
	 * @param importMappings character import mappings
	 */
	public ImportAssets(String url, ImportMapping[] importMappings) {
		code				    = TagConstants.IMPORT_ASSETS;
		this.url			    = url;
		this.importMappings     = importMappings;
	}

	ImportAssets() {
		// empty
	}

	/**
	 * Sets the import mappings defined in this tag.
	 *
	 * @param importMappings character import mappings
	 */
	public void setImportMappings(ImportMapping[] importMappings) {
		this.importMappings = importMappings;
	}

	/**
	 * Returns the import mappings defined in this tag.
	 *
	 * @return character import mappings
	 */
	public ImportMapping[] getImportMappings() {
		return importMappings;
	}

	/**
	 * Sets the URL of the SWF file exporting the characters.
	 *
	 * @param url URL of import source
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Returns the URL of the SWF file exporting the characters.
	 *
	 * @return URL of import source
	 */
	public String getUrl() {
		return url;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeString(url);
		int count = importMappings.length;
		outStream.writeUI16(count);
		for (int i = 0; i < count; i++) {
			outStream.writeUI16(importMappings[i].getCharacterId());
			outStream.writeString(importMappings[i].getName());
		}
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
    if (getSWFVersion() < 6) {
      if (isJapanese()) {
        inStream.setShiftJIS(true);
      } else {
        inStream.setANSI(true);
      }
    }
		url = inStream.readString();
		int count = inStream.readUI16();
		importMappings = new ImportMapping[count];
		for (int i = 0; i < count; i++) {
			int characterId   = inStream.readUI16();
			String name		  = inStream.readString();
			importMappings[i] = new ImportMapping(name, characterId);
		}
	}

	/**
	 * Defines an (immutable) import mapping for a character, containing its
	 * export name and the ID the character instance gets after import.
	 */
	public static class ImportMapping implements Serializable {
		private int characterId;
		private String name;

		/**
		 * Creates a new import mapping. Supply export name of character and ID
		 * of imported instance.
		 *
		 * @param name export name of imported character
		 * @param characterId imported instance ID
		 */
		public ImportMapping(String name, int characterId) {
			this.name			 = name;
			this.characterId     = characterId;
		}

		/**
		 * Returns the imported character instance's ID.
		 *
		 * @return character ID
		 */
		public int getCharacterId() {
			return characterId;
		}

		/**
		 * Returns the export name of the imported character.
		 *
		 * @return export name
		 */
		public String getName() {
			return name;
		}
	}
}
