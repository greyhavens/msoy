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


/**
 * <p>
 * This tag defines an event sound.
 * </p>
 * 
 * <p>
 * Warning: you are responsible for obtaining technology licenses needed for
 * encoding and decoding sound data (see e.g. <a
 * href="http://mp3licensing.com">mp3licensing.com</a> for details on mp3
 * licensing).
 * </p>
 *
 * @since SWF 1
 */
public final class DefineSound extends DefinitionTag {
	/**
	 * Uncompressed sound format. For 16-bit samples, native byte ordering
	 * (little-endian or big-endian) is used. Warning: this introduces
	 * platform dependency!
	 */
	public static final byte FORMAT_UNCOMPRESSED			   = 0;
	/**
	 * ADPCM compressed sound format (simple compression algorithm without
	 * licensing issues).
	 */
	public static final byte FORMAT_ADPCM					   = 1;
	/**
	 * mp3 compressed sound format (for high-quality sound encoding) (since SWF
	 * 4).
	 */
	public static final byte FORMAT_MP3						   = 2;
	/**
	 * Uncompressed little-endian sound format, i.e. 16-bit samples are decoded
	 * using little-endian byte ordering (platform-independent format) (since
	 * SWF 4).
	 */
	public static final byte FORMAT_UNCOMPRESSED_LITTLE_ENDIAN = 3;
	/**
	 * Nellymoser Asao compressed sound format (optimized for low-bitrate mono
	 * speech transmission) (since SWF 6).
	 */
	public static final byte FORMAT_NELLYMOSER				   = 6;
	/** 5.5 kHz sampling rate */
	public static final byte RATE_5500_HZ					   = 0;
	/** 11 kHz sampling rate */
	public static final byte RATE_11000_HZ					   = 1;
	/** 22 kHz sampling rate */
	public static final byte RATE_22000_HZ					   = 2;
	/** 44 kHz sampling rate */
	public static final byte RATE_44000_HZ					   = 3;
	private byte format;
	private byte rate;
	private boolean is16BitSample;
	private boolean isStereo;
	private long sampleCount;
	private byte[] soundData;

	/**
	 * Creates a new DefineSound instance. Supply the character ID of the
	 * sound, the encoding format (one of the provided <code>FORMAT_...</code>
	 * constants), the sampling rate (use <code>RATE_...</code> constants),
	 * specify whether 8-bit or 16-bit samples are used (8-bit samples are
	 * allowed only for uncompressed formats) and whether the sound is mono or
	 * stereo (Nellymoser merely supports mono). Provide the number of samples
	 * (for stereo sound: sample pairs) and, finally, the actual sound data as
	 * raw data.
	 *
	 * @param characterId character ID of sound
	 * @param format encoding format (use provided constants)
	 * @param rate sampling rate (use provided constants)
	 * @param is16BitSample if <code>true</code>, 16-bit samples are used
	 * 		  (otherwise 8-bit, for uncompressed formats only)
	 * @param isStereo if <code>true</code>, sound is stereo, otherwise mono
	 * @param sampleCount number of samples (stereo: sample pairs)
	 * @param soundData raw sound data
	 */
	public DefineSound(
		int characterId, byte format, byte rate, boolean is16BitSample,
		boolean isStereo, long sampleCount, byte[] soundData) {
		code				   = TagConstants.DEFINE_SOUND;
		this.characterId	   = characterId;
		this.format			   = format;
		this.rate			   = rate;
		this.is16BitSample     = is16BitSample;
		this.isStereo		   = isStereo;
		this.sampleCount	   = sampleCount;
		this.soundData		   = soundData;
	}

	DefineSound() {
		// empty
	}

	/**
	 * Sets the encoding format of the sound (one of the
	 * <code>FORMAT_...</code> constants).
	 *
	 * @param format sound encoding format
	 */
	public void setFormat(byte format) {
		this.format = format;
	}

	/**
	 * Returns the encoding format of the sound (one of the
	 * <code>FORMAT_...</code> constants).
	 *
	 * @return sound encoding format
	 */
	public byte getFormat() {
		return format;
	}

	/**
	 * Returns the sampling rate of the sound (one of the <code>RATE_...</code>
	 * constants).
	 *
	 * @return sampling rate
	 */
	public byte getRate() {
		return rate;
	}

	/**
	 * Sets the sound's number of samples (for stereo sound: sample pairs).
	 *
	 * @param sampleCount sample count
	 */
	public void setSampleCount(long sampleCount) {
		this.sampleCount = sampleCount;
	}

	/**
	 * Returns the sound's number of samples (for stereo sound: sample pairs).
	 *
	 * @return sample count
	 */
	public long getSampleCount() {
		return sampleCount;
	}

	/**
	 * Sets the sound data.
	 *
	 * @param soundData raw sound data (as byte array)
	 */
	public void setSoundData(byte[] soundData) {
		this.soundData = soundData;
	}

	/**
	 * Returns the sound data.
	 *
	 * @return raw sound data (as byte array)
	 */
	public byte[] getSoundData() {
		return soundData;
	}

	/**
	 * Specifies whether the sound is stereo or not.
	 *
	 * @param isStereo <code>true</code> if stereo, otherwise
	 * 		  <code>false</code>
	 */
	public void setStereo(boolean isStereo) {
		this.isStereo = isStereo;
	}

	/**
	 * Checks whether the sound is stereo or not.
	 *
	 * @return <code>true</code> if stereo, otherwise <code>false</code>
	 */
	public boolean isStereo() {
		return isStereo;
	}

	/**
	 * Checks whether the sample size is 16 bit or 8 bit.
	 *
	 * @return <code>true</code> if 16-bit sample size, <code>false</code> if
	 * 		   8-bit
	 */
	public boolean is16BitSample() {
		return is16BitSample;
	}

	/**
	 * Specifies whether the sample size is 16 bit or 8 bit.
	 *
	 * @param is16BitSample <code>true</code> if 16-bit sample size,
	 * 		  <code>false</code> if 8-bit
	 */
	public void set16BitSample(boolean is16BitSample) {
		this.is16BitSample = is16BitSample;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(characterId);
		outStream.writeUnsignedBits(format, 4);
		outStream.writeUnsignedBits(rate, 2);
		outStream.writeBooleanBit(is16BitSample);
		outStream.writeBooleanBit(isStereo);
		outStream.writeUI32(sampleCount);
		outStream.writeBytes(soundData);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		characterId		  = inStream.readUI16();
		format			  = (byte) inStream.readUnsignedBits(4);
		rate			  = (byte) inStream.readUnsignedBits(2);
		is16BitSample     = inStream.readBooleanBit();
		isStereo		  = inStream.readBooleanBit();
		sampleCount		  = inStream.readUI32();
		// that's 7 bytes
		soundData		  = new byte[data.length - 7];
		System.arraycopy(data, 7, soundData, 0, data.length - 7);
	}
}
