/**
 * Copyright Copyright 2015 Piero Dalle Pezze
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

package uk.ac.babraham.BamQC.Modules;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.xml.stream.XMLStreamException;

import net.sf.samtools.AlignmentBlock;
import net.sf.samtools.SAMRecord;
import uk.ac.babraham.BamQC.Annotation.AnnotationSet;
import uk.ac.babraham.BamQC.Annotation.Chromosome;
import uk.ac.babraham.BamQC.Graphs.HorizontalBarGraph;
import uk.ac.babraham.BamQC.Report.HTMLReportArchive;
import uk.ac.babraham.BamQC.Sequence.SequenceFile;
import uk.ac.babraham.BamQC.Utilities.CigarMDGenerator;
import uk.ac.babraham.BamQC.Utilities.CigarMDElement;
import uk.ac.babraham.BamQC.Utilities.CigarMDOperator;
import uk.ac.babraham.BamQC.Utilities.CigarMD;






public class SNPFrequencies extends AbstractQCModule {

	// data fields for statistics
	private long ac = 0;
	private long ag = 0;
	private long at = 0;
	private long ca = 0;
	private long cg = 0;
	private long ct = 0;
	private long ga = 0;
	private long gc = 0;
	private long gt = 0;
	private long ta = 0;
	private long tc = 0;
	private long tg = 0;
	private long totalMutations = 0;	
	private long aInsertions = 0;
	private long cInsertions = 0;
	private long gInsertions = 0;
	private long tInsertions = 0;
	private long nInsertions = 0;		
	private long totalInsertions = 0;
	private long aDeletions = 0;
	private long cDeletions = 0;
	private long gDeletions = 0;
	private long tDeletions = 0;
	private long nDeletions = 0;	
	private long totalDeletions = 0;	
	private long totalMatches = 0;
	private long total = 0;
	
	private long readSkippedRegions = 0;
	private long referenceSkippedRegions = 0;
	
    private long skippedReads = 0;
    private long totalReads = 0;	
    
	
	// Used for computing the statistics 
	private CigarMDGenerator cigarMDGenerator = new CigarMDGenerator();

	private CigarMD cigarMD = new CigarMD();
	private CigarMDElement currentCigarMDElement = null;
	
	
	
	// Constructors
	/**
	 * Default constructor
	 */
	public SNPFrequencies() { }

	
	
	
	// @Override methods
	
	@Override
	public void processSequence(SAMRecord read) {

		totalReads++;
		
		// Compute and get the CigarMD object combining the strings Cigar and MD tag
		cigarMDGenerator.generateCigarMD(read);
		cigarMD = cigarMDGenerator.getCigarMD();
				
		if(cigarMD.isEmpty()) {
			skippedReads++;
			return;			
		}

		// Iterate the CigarMDElements list to collect statistics
		List<CigarMDElement> cigarMDElements = cigarMD.getCigarMDElements();
		Iterator<CigarMDElement> cigarMDIter = cigarMDElements.iterator();
		CigarMDOperator currentCigarMDElementOperator;

		
		while(cigarMDIter.hasNext()) {
			currentCigarMDElement = cigarMDIter.next();

			currentCigarMDElementOperator = currentCigarMDElement.getOperator();
			
			// debugging
			//System.out.println("Parsing CigarElement: " + String.valueOf(currentCigarElementLength) + currentCigarElementOperator.toString());
			if(currentCigarMDElementOperator.equals(CigarMDOperator.MATCH)) {
				processMDtagCigarOperatorM();
			} else if(currentCigarMDElementOperator.equals(CigarMDOperator.MISMATCH)) {
				processMDtagCigarOperatorU();
			} else if(currentCigarMDElementOperator.equals(CigarMDOperator.INSERTION)) {
				processMDtagCigarOperatorI();
			} else if(currentCigarMDElementOperator.equals(CigarMDOperator.DELETION)) {
				processMDtagCigarOperatorD();				
			} else if(currentCigarMDElementOperator.equals(CigarMDOperator.SKIPPED_REGION)) {
				//processMDtagCigarOperatorN();
				//System.out.println("SNPFrequencies.java: extended CIGAR element N is currently unsupported.");
				skippedReads++;
				break;
			} else if(currentCigarMDElementOperator.equals(CigarMDOperator.SOFT_CLIP)) {
				//System.out.println("SNPFrequencies.java: extended CIGAR element S is currently unsupported.");
				skippedReads++;
				break;
			} else if(currentCigarMDElementOperator.equals(CigarMDOperator.HARD_CLIP)) {
				//System.out.println("SNPFrequencies.java: extended CIGAR element H is currently unsupported.");
				skippedReads++;
				break;
			} else if(currentCigarMDElementOperator.equals(CigarMDOperator.PADDING)) {
				//System.out.println("SNPFrequencies.java: extended CIGAR element P is currently unsupported.");
				skippedReads++;
				break;
			} else if(currentCigarMDElementOperator.equals(CigarMDOperator.eq)) {
				//System.out.println("SNPFrequencies.java: extended CIGAR element = is currently unsupported.");
				skippedReads++;
				break;
			} else if(currentCigarMDElementOperator.equals(CigarMDOperator.x)) {
				//System.out.println("SNPFrequencies.java: extended CIGAR element X is currently unsupported.");
				skippedReads++;
				break;
			} else {
				//System.out.println("SNPFrequencies.java: Unknown operator in the CIGAR string.");
				skippedReads++;
				break;
			}		
		}
				
		computeTotals();
//		debugging
//		System.out.println("Combined Cigar MDtag: " + cigarMD.toString());
	}
	
	
	@Override	
	public void processFile(SequenceFile file) {}

	@Override	
	public void processAnnotationSet(AnnotationSet annotation) {}

	@Override	
	public JPanel getResultsPanel() {
		return null;
	}

	@Override	
	public String name() {
		return "SNP Frequencies";
	}

	@Override	
	public String description() {
		return "Looks at the overall SNP frequencies in the data";
	}

	@Override	
	public void reset() {
		ac = 0;
		ag = 0;
		at = 0;
		ca = 0;
		cg = 0;
		ct = 0;
		ga = 0;
		gc = 0;
		gt = 0;
		ta = 0;
		tc = 0;
		tg = 0;
		totalMutations = 0;
		aInsertions = 0;
		cInsertions = 0;
		gInsertions = 0;
		tInsertions = 0;
		nInsertions = 0;
		totalInsertions = 0;
		aDeletions = 0;
		cDeletions = 0;
		gDeletions = 0;
		tDeletions = 0;
		nDeletions = 0;
		totalDeletions = 0;
		totalMatches = 0;
		total = 0;
		skippedReads = 0;
		totalReads = 0;

		readSkippedRegions = 0;
		referenceSkippedRegions = 0;

		cigarMD = new CigarMD();
	}

	@Override	
	public boolean raisesError() {
		//TODO: Set this
		return false;
	}

	@Override	
	public boolean raisesWarning() {
		//TODO: Set this
		return false;
	}

	@Override	
	public boolean needsToSeeSequences() {
		return true;
	}

	@Override	
	public boolean needsToSeeAnnotation() {
		return false;
	}

	@Override	
	public boolean ignoreInReport() {
		return total == 0;
	}

	@Override	
	public void makeReport(HTMLReportArchive report) throws XMLStreamException, IOException {
		// TODO Auto-generated method stub

	}	 

	

	
	
	// Private methods here
	
	/* Compute the totals */
	private void computeTotals() {
		totalMutations = ac+ag+at+
						  ca+cg+ct+
						  ga+gc+gt+
						  ta+tc+tg;
		// NOTE: nInsertions and nDeletions are not counted in the totals. 
		totalInsertions = aInsertions + cInsertions + gInsertions + tInsertions;
		totalDeletions = aDeletions + cDeletions + gDeletions + tDeletions;
		total = totalMutations + totalInsertions + totalDeletions;
	}
	
	

	// These methods process the combined CigarMD object.
	
	/* Process the MD string once found the CigarMD operator m (match). */
	private void processMDtagCigarOperatorM() {
		totalMatches = totalMatches + currentCigarMDElement.getLength();
	}
	
	/* Process the MD string once found the CigarMD operator u (mismatch). 
	 * So far this element is indicated as 1u{ACGT}ref{ACGT}read
	 * to indicate a mutation from reference to read.
	 * In the future the length will correspond to the number of adjacent mutations.
	 * e.g. 3uACGTAT will indicate that the substring AGA on the reference has been 
	 * mutated in CTT.
	 */
	private void processMDtagCigarOperatorU() {
		int numMutations = currentCigarMDElement.getLength();
		String mutatedBases = currentCigarMDElement.getBases();
		String basePair = "";
		
		// debugging
		//System.out.println("SNPFrequencies.java - " + currentCigarMDElement + " : " + cigarMD.toString());
		//System.out.println(mutatedBases);
		
		for(int i = 0; i < numMutations; i++) {
			basePair = mutatedBases.substring(i*2, i*2+2);
			if(basePair.equals("AC")) { ac++; }
			else if(basePair.equals("AG")) { ag++; }
			else if(basePair.equals("AT")) { at++; }
			else if(basePair.equals("CA")) { ca++; }
			else if(basePair.equals("CG")) { cg++; }
			else if(basePair.equals("CT")) { ct++; }
			else if(basePair.equals("GA")) { ga++; }
			else if(basePair.equals("GC")) { gc++; }
			else if(basePair.equals("GT")) { gt++; }
			else if(basePair.equals("TA")) { ta++; }
			else if(basePair.equals("TC")) { tc++; }
			else if(basePair.equals("TG")) { tg++; }	
			else if(basePair.charAt(0) == 'N') { referenceSkippedRegions++; }
			else if(basePair.charAt(1) == 'N') { readSkippedRegions++; }			
		}
	}	
	
	/* Process the MD string once found the CigarMD operator i (insertion). */	
	private void processMDtagCigarOperatorI() {
		int numInsertions = currentCigarMDElement.getLength();
		String insertedBases = currentCigarMDElement.getBases();
		for(int i = 0; i < numInsertions; i++) {
			if(insertedBases.charAt(i) == 'A') { aInsertions++; }
			else if(insertedBases.charAt(i) == 'C') { cInsertions++; }
			else if(insertedBases.charAt(i) == 'G') { gInsertions++; }
			else if(insertedBases.charAt(i) == 'T') { tInsertions++; }
			else if(insertedBases.charAt(i) == 'N') { nInsertions++; }			
		}
	}
	
	/* Process the MD string once found the CigarMD operator d (deletion). */	
	private void processMDtagCigarOperatorD() {
		int numDeletions = currentCigarMDElement.getLength();
		String deletedBases = currentCigarMDElement.getBases();
		for(int i = 0; i < numDeletions; i++) {
			if(deletedBases.charAt(i) == 'A') { aDeletions++; }
			else if(deletedBases.charAt(i) == 'C') { cDeletions++; }
			else if(deletedBases.charAt(i) == 'G') { gDeletions++; }
			else if(deletedBases.charAt(i) == 'T') { tDeletions++; }
			else if(deletedBases.charAt(i) == 'N') { nDeletions++; }			
		}
	}
	
	
	// Have to test the following code.
	
	/* Process the MD string once found the CigarMD operator n. */	
	private void processMDtagCigarOperatorN() {
		readSkippedRegions = readSkippedRegions + currentCigarMDElement.getLength();
	}
	
	/* Process the MD string once found the CigarMD operator s. */	
	private void processMDtagCigarOperatorS() {}
	
	/* Process the MD string once found the CigarMD operator h. */	
	private void processMDtagCigarOperatorH() {}
	
	/* Process the MD string once found the CigarMD operator p. */
	private void processMDtagCigarOperatorP() {}	
	
	/* Process the MD string once found the CigarMD operator =. */	
	private void processMDtagCigarOperatorEQ() {}	
	
	/* Process the MD string once found the CigarMD operator X. */	
	private void processMDtagCigarOperatorNEQ() {}

	
	
	
	
	
	
	
	
	// Getter methods
	
	public CigarMD getCigarMD() {
		return cigarMD;
	}

	public long getA2C() {
		return ac;
	}

	public long getA2G() {
		return ag;
	}

	public long getA2T() {
		return at;
	}

	public long getC2A() {
		return ca;
	}

	public long getC2G() {
		return cg;
	}

	public long getC2T() {
		return ct;
	}



	public long getG2A() {
		return ga;
	}

	public long getG2C() {
		return gc;
	}

	public long getG2T() {
		return gt;
	}

	public long getT2A() {
		return ta;
	}

	public long getT2C() {
		return tc;
	}

	public long getT2G() {
		return tg;
	}

	public long getTotalMutations() {
		return totalMutations;
	}

	public long getAInsertions() {
		return aInsertions;
	}

	public long getCInsertions() {
		return cInsertions;
	}

	public long getGInsertions() {
		return gInsertions;
	}

	public long getTInsertions() {
		return tInsertions;
	}

	public long getNInsertions() {
		return nInsertions;
	}
	
	public long getTotalInsertions() {
		return totalInsertions;
	}

	public long getADeletions() {
		return aDeletions;
	}

	public long getCDeletions() {
		return cDeletions;
	}

	public long getGDeletions() {
		return gDeletions;
	}

	public long getTDeletions() {
		return tDeletions;
	}
	
	public long getNDeletions() {
		return nDeletions;
	}

	public long getTotalDeletions() {
		return totalDeletions;
	}

	public long getTotalMatches() {
		return totalMatches;
	}

	public long getTotal() {
		return total;
	}

	public long getReadSkippedRegions() {
		return readSkippedRegions;
	}	
	
	public long getReferenceSkippedRegions() {
		return referenceSkippedRegions;
	}	
	
	public long getSkippedReads() {
		return skippedReads;
	}	
	
	public long getTotalReads() {
		return totalReads;
	}	
		
	
	
	
	
	
	
	
	
	
	
	
	
}
