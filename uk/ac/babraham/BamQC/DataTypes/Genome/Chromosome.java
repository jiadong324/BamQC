/**
 * Copyright Copyright 2014 Simon Andrews
 *
 *    This file is part of BamQC.
 *
 *    BamQC is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    BamQC is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with BamQC; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
/*
 * Changelog: 
 * - Piero Dalle Pezze: Optimised data structures, optimised algorithm, use of system library.
 * - Simon Andrews: Class creation.
 */
package uk.ac.babraham.BamQC.DataTypes.Genome;

import java.io.Serializable;


/** 
 * @author Simon Andrews
 * @author Piero Dalle Pezze
 *
 */
public class Chromosome implements Comparable<Chromosome>, Serializable {


	private static final long serialVersionUID = 6553668167050794944L;

	public static int COVERAGE_BIN_SIZE = 1000000;
	
	private String name;
	private int length = 1;
	
	private long [] coverageBins = new long[0];
	
	// These are values we're going to store for a specific analysis
	private int seqCount = 0;
	
	public Chromosome (String name) {
		this.name = name;
	}
	
	public String name () {
		return name;
	}
	
	public void setLength (int length) {
		this.length = length;
		
		int maxBin = length/COVERAGE_BIN_SIZE;
				
		if (coverageBins.length<=maxBin) {
			long[] oldCoverageBins = coverageBins;
			coverageBins = new long[maxBin+1];	
			System.arraycopy(oldCoverageBins, 0, coverageBins, 0, oldCoverageBins.length);
		}
		
	}
	
	public long [] getBinCountData () {
		return coverageBins;
	}
	
	public int length () {
		return length;
	}
		
	public int seqCount() {
		return seqCount;
	}
	
	public void processSequence (ShortRead record) {
		seqCount++;
		
		if (record.getAlignmentEnd() > length) {
			length = record.getAlignmentEnd();
		}
		
		int maxBin = record.getAlignmentStart()/COVERAGE_BIN_SIZE;
		
		if (coverageBins.length<=maxBin) {
			long[] oldCoverageBins = coverageBins;
			coverageBins = new long[maxBin+1];
			System.arraycopy(oldCoverageBins, 0, coverageBins, 0, oldCoverageBins.length);
		}
		coverageBins[maxBin]++;
		
	}
	
	@Override
	public int compareTo(Chromosome o) {
		
		// We try to compare by number first and then by string if that
		// fails.  Numbers come before text.
		//
		// Because the manual genome builder adds a 'pseudo' prefix to 
		// the artificial chromosomes it builds we remove that if it's 
		// present so that doesn't mess up the ordering.
		//
		// We also remove 'chr' if present.
		//
		// This needed to be modified to fix a nasty data corruption bug.
		// The sorting rule actually needs to be:
		//
		// If they're both integers, sort as integers
		//
		// If they're both text, sort as text
		//
		// If one is an integer and the other is text then the integer wins
		
		String thisName = name;
		String thatName = o.name;
		
		if (thisName.startsWith("pseudo")) {
			thisName = thisName.substring(6);
		}
		if (thatName.startsWith("pseudo")) {
			thatName = thatName.substring(6);
		}		
		
		if (thisName.toLowerCase().startsWith("chr")) {
			thisName = thisName.substring(3);
		}
		if (thatName.toLowerCase().startsWith("chr")) {
			thatName = thatName.substring(3);
		}
		
		try {
			int thisNumber = Integer.parseInt(thisName);
			try {
				int thatNumber = Integer.parseInt(thatName);
				
				// They're both numbers
				return thisNumber - thatNumber;
			}
			catch (NumberFormatException e) {
				// That is text, this is number, this wins
				return -1;
			}
		}
		catch (NumberFormatException e) {
			try {
				Integer.parseInt(thatName);
				
				// This is text, that is a number, that wins
				return 1;
			}
			catch (NumberFormatException e2) {
				// They're both text
				return thisName.compareTo(thatName);
			}
			
		}
				
	}
	
}
