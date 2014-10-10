/**
 * Copyright Copyright 2014 Bart Ailey Eagle Genomics Ltd
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.xml.stream.XMLStreamException;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;

import org.apache.log4j.Logger;

import uk.ac.babraham.BamQC.Annotation.AnnotationSet;
import uk.ac.babraham.BamQC.Graphs.LineGraph;
import uk.ac.babraham.BamQC.Report.HTMLReportArchive;
import uk.ac.babraham.BamQC.Sequence.SequenceFile;

public class GenomeCoverage extends AbstractQCModule {

	private static Logger log = Logger.getLogger(GenomeCoverage.class);
	private static int BIN_NUCLEOTIDES = 50000;

	private List<float[]> coverage = new ArrayList<float[]>();
	private List<int[]> binSize = new ArrayList<int[]>();
	private double maxCoverage;
	private boolean isHeaderData = false;

	public static void setBinNucleotides(int binNucleotides) {
		BIN_NUCLEOTIDES = binNucleotides;
	}
	
	private float[] getNewReadReferenceCoverage(int referenceIndex, SAMFileHeader header) {
		SAMSequenceRecord samSequenceRecord = header.getSequence(referenceIndex);
		int referenceSequenceLength = samSequenceRecord.getSequenceLength();
		int coverageLength = (referenceSequenceLength / BIN_NUCLEOTIDES);
		int modulus = referenceSequenceLength % BIN_NUCLEOTIDES;

		if (modulus != 0) coverageLength++;

		int[] referenceBinSize = new int[coverageLength];
		for (int i = 0; i < coverageLength; i++) {
			referenceBinSize[i] = BIN_NUCLEOTIDES;
		}
		if (modulus != 0) {
			referenceBinSize[coverageLength - 1] = modulus;
		}

		if (binSize.size() <= referenceIndex) {
			for (int i = binSize.size(); i <= referenceIndex; i++) {
				binSize.add(null);
			}
		}
		binSize.set(referenceIndex, referenceBinSize);

		return new float[coverageLength];
	}

	private float[] getReadReferenceCoverage(int referenceIndex, SAMFileHeader header) {
		float[] readReferenceCoverage = null;

		if (referenceIndex < coverage.size()) {
			readReferenceCoverage = coverage.get(referenceIndex);
		}
		else {
			readReferenceCoverage = getNewReadReferenceCoverage(referenceIndex, header);

			if (referenceIndex >= coverage.size()) {
				for (int i = coverage.size(); i <= referenceIndex; i++) {
					coverage.add(null);
				}
			}
			coverage.set(referenceIndex, readReferenceCoverage);
		}
		return readReferenceCoverage;
	}

	private void recordCoverage(long alignmentStart, long alignmentEnd, float[] readReferenceCoverage, int[] referenceBinSize) {
		int startIndex = (int) alignmentStart / BIN_NUCLEOTIDES;
		int endIndex = (int) alignmentEnd / BIN_NUCLEOTIDES;
		int index = startIndex;

		while (index <= endIndex) {
			long binStart = index * BIN_NUCLEOTIDES;
			long binEnd = (index + 1) * BIN_NUCLEOTIDES;
			long start = alignmentStart > binStart ? alignmentStart : binStart;
			long end = alignmentEnd > binEnd ? binEnd : alignmentEnd;
			float length = (float) (end - start);
			float binCoverage = length / referenceBinSize[index];

			readReferenceCoverage[index] += binCoverage;

			if (readReferenceCoverage[index] > maxCoverage) maxCoverage = readReferenceCoverage[index];

			log.debug(String.format("Start %d - End %d, index %d, binCoverage %f, ", alignmentStart, alignmentEnd, index, binCoverage, readReferenceCoverage[index]));

			index++;
		}
	}

	@Override
	public void processSequence(SAMRecord read) {
		SAMFileHeader header = read.getHeader();
		int referenceIndex = read.getReferenceIndex();
		long alignmentStart = read.getAlignmentStart();
		long alignmentEnd = read.getAlignmentEnd();
		float[] readReferenceCoverage = getReadReferenceCoverage(referenceIndex, header);
		int[] referenceBinSize = binSize.get(referenceIndex);

		recordCoverage(alignmentStart, alignmentEnd, readReferenceCoverage, referenceBinSize);

		log.debug("header = " + header);
		log.debug("referenceIndex = " + referenceIndex);
	}

	@Override
	public void processFile(SequenceFile file) {
		log.info("processFile called");
	}

	@Override
	public String name() {
		return "Genome Coverage";
	}

	@Override
	public String description() {
		return "Genome Coverage";
	}

	@Override
	public void reset() {
		coverage = new ArrayList<float[]>();
		binSize = new ArrayList<int[]>();
		isHeaderData = false;
	}

	@Override
	public boolean raisesError() {
		return false;
	}

	@Override
	public boolean raisesWarning() {
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
		return false;
	}

	@Override
	public void processAnnotationSet(AnnotationSet annotation) {
		throw new UnsupportedOperationException("processAnnotationSet called");
	}

	@Override
	public JPanel getResultsPanel() {
		double[][] coverageData = getCoverageData();
		double minY = 0.0D;
		double maxY = maxCoverage;
		String xLabel = String.format("Bin size %6.1E nucleotides", (double) BIN_NUCLEOTIDES);
		String[] xTitles = new String[] { "" };
		int[] xCategories = new int[coverageData[0].length];
		String graphTitle = "Reference Coverage";

		for (int i = 0; i < coverageData.length; i++) {
			xCategories[i] = i;
		}
		log.info("maxCoverage = " + maxCoverage);
		log.info("xCategories.length = " + xCategories.length);

		return new LineGraph(coverageData, minY, maxY, xLabel, xTitles, xCategories, graphTitle);
	}

	private double[][] getCoverageData() {
		List<Float> data = new ArrayList<Float>();

		log.info("coverage = " + coverage);
		log.info("coverage.size = " + coverage.size());

		for (float[] referenceCoverage : coverage) {
			if (referenceCoverage != null) {
				log.info("referenceCoverage = " + referenceCoverage);
				
				for (float binCoverage : referenceCoverage) {
					data.add(binCoverage);
				}
			}
		}
		double[][] coverageData = new double[1][data.size()];
		int i = 0;

		for (float binCoverage : data) {
			coverageData[0][i++] = binCoverage;
		}
		return coverageData;
	}

	@Override
	public void makeReport(HTMLReportArchive report) throws XMLStreamException, IOException {
		// TODO Auto-generated method stub
	}

	public List<float[]> getCoverage() {
		return coverage;
	}

}
