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
 * This tag makes one or more defined characters available for import by other
 * SWF files. Exported characters can be imported with the
 * <code>ImportAssets</code> tag.
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
 * @see ImportAssets
 * @since SWF 5
 */
public final class ExportAssets extends Tag {
	private ExportMapping[] exportMappings;

	/**
	 * Creates a new ExportAssets instance. Supply an array of export mappings
	 * (for each  exported character one).
	 *
	 * @param exportMappings character export mappings
	 */
	public ExportAssets(ExportMapping[] exportMappings) {
		code				    = TagConstants.EXPORT_ASSETS;
		this.exportMappings     = exportMappings;
	}

	ExportAssets() {
		// empty
	}

	/**
	 * Sets the export mappings defined in this tag.
	 *
	 * @param exportMappings character export mappings
	 */
	public void setExportMappings(ExportMapping[] exportMappings) {
		this.exportMappings = exportMappings;
	}

	/**
	 * Returns the export mappings defined in this tag.
	 *
	 * @return character export mappings
	 */
	public ExportMapping[] getExportMappings() {
		return exportMappings;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		int count = exportMappings.length;
		outStream.writeUI16(count);
		for (int i = 0; i < count; i++) {
			outStream.writeUI16(exportMappings[i].getCharacterId());
			outStream.writeString(exportMappings[i].getName());
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
		int count			    = inStream.readUI16();
		exportMappings		    = new ExportMapping[count];
		for (int i = 0; i < count; i++) {
			exportMappings[i] = new ExportMapping(
					inStream.readUI16(), inStream.readString());
		}
	}

	/**
	 * Defines an (immutable) export mapping for a character to be exported,
	 * containing its ID and its export name.
	 */
	public static class ExportMapping implements Serializable {
		private int characterId;
		private String name;

		/**
		 * Creates a new export mapping. Supply ID of exported character and
		 * export name.
		 *
		 * @param characterId character ID
		 * @param name export name
		 */
		public ExportMapping(int characterId, String name) {
			this.characterId     = characterId;
			this.name			 = name;
		}

		/**
		 * Returns the ID of the exported character.
		 *
		 * @return character ID
		 */
		public int getCharacterId() {
			return characterId;
		}

		/**
		 * Returns the export name of the character.
		 *
		 * @return export name
		 */
		public String getName() {
			return name;
		}
	}
}
