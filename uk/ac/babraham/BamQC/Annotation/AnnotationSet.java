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

package uk.ac.babraham.BamQC.Annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import uk.ac.babraham.BamQC.Modules.ModuleConfig;
import net.sf.samtools.SAMRecord;

public class AnnotationSet {
	
	private ChromosomeFactory factory = new ChromosomeFactory();
	
	private HashMap<String, FeatureClass> features = new HashMap<String, FeatureClass>();
	
	private FeatureClass [] featureArray = null;
	
	private final int cacheSize = ModuleConfig.getParam("feature_coverage_annotation_cache_size", "ignore").intValue();
	private List<ShortRead> readCache = new ArrayList<ShortRead>(cacheSize);
	//private List<TempSAMRecord> tempSAMRecordCache = new ArrayList<TempSAMRecord>(cacheSize);
	
	
	public ChromosomeFactory chromosomeFactory () {
		return factory;
	}
	
	public void addFeature (Feature f) {
		
		if (!features.containsKey(f.type())) {
			features.put(f.type(), new FeatureClass(this));
		}
		
		features.get(f.type()).addFeature(f);
		
	}
	
	public boolean hasFeatures () {
		return !features.isEmpty();
	}
	
	public String [] listFeatureTypes () {
		return features.keySet().toArray(new String [0]);
	}
	
	public FeatureClass getFeatureClassForType (String type) {
		return features.get(type);
	}
	
	
	
	
	

	public void processSequence (SAMRecord r) {
			
		// implementation using ShortRead
	    if(readCache.size() < cacheSize) {
	    	readCache.add(new ShortRead(r.getReferenceName(), r.getAlignmentStart(), r.getAlignmentEnd()));
	    } else {
	    	// sort the cache
	    	Collections.sort(readCache);
	    	// now parse the sorted cache
	    	for(int i=0; i < cacheSize; i++) {
	    		processCachedSequence(readCache.get(i));
	    	}
	    	// let's clear and reuse the array for now, instead of reallocating a new one every time.
	    	// Tricky to say what's the best is.. an O(n)remove vs allocation+GC ... 
	    	readCache.clear();
	    }
	        
		
//		// implementation using SAMRecord
//	    if(tempSAMRecordCache.size() < cacheSize) {
//	    	tempSAMRecordCache.add(new TempSAMRecord(r));
//	    } else {
//	    	// sort the cache
//	    	Collections.sort(tempSAMRecordCache);
//	    	// now parse the sorted cache
//	    	for(int i=0; i < cacheSize; i++) {
//	    		processCachedSequence(tempSAMRecordCache.get(i).getSAMRecord());
//	    	}
//	    	// let's clear and reuse the array for now, instead of reallocating a new one every time.
//	    	// Tricky to say what's the best is.. an O(n)remove vs allocation+GC ... 
//	    	tempSAMRecordCache.clear();
//	    }
	}
	
	
	/** SOLUTION CHANGING THE DATA STRUCTURE SAMRecord to ShortRead */	
	private void processCachedSequence(ShortRead r) {	
		if (!r.getReferenceName().equals("*")) {
			Chromosome c = factory.getChromosome(r.getReferenceName());
			c.processSequence(r);
		}
		if (featureArray == null) {
			featureArray = features.values().toArray(new FeatureClass[0]);
		}
		for (int i=0;i<featureArray.length;i++) {
			featureArray[i].processSequence(r);
		}
	}	
	
	
	
	/** SOLUTION WITHOUT CHANGING THE DATA STRUCTURE SAMRecord */

	/**
	 * A temporary class storing a SAMRecord. 
	 * It is used for cache. Future implementations will use 
	 * ShortRead instead, but for now, we don't want to reimplement 
	 * all the processSequence(SAMREcord) methods of the other classes, 
	 * before testing whether or not this is a convenient solution.
	 */
	@Deprecated
	class TempSAMRecord implements Comparable<TempSAMRecord> {
		private SAMRecord samRecord;
		public TempSAMRecord(SAMRecord r) {
			samRecord = r;
		}
		@Override
		public int compareTo(TempSAMRecord sr) {
			int compareTest = samRecord.getReferenceName().compareTo(sr.getSAMRecord().getReferenceName());
			if(compareTest == 0) {
				return Integer.valueOf(samRecord.getAlignmentStart()).compareTo(Integer.valueOf(sr.getSAMRecord().getAlignmentStart()));
			}
			return compareTest;
		}
		public SAMRecord getSAMRecord() { 
			return samRecord;
		}
	}	
	
	@Deprecated
	private void processCachedSequence(SAMRecord r) {
		if (!r.getReferenceName().equals("*")) {
			Chromosome c = factory.getChromosome(r.getReferenceName());
			c.processSequence(r);
		}
		if (featureArray == null) {
			featureArray = features.values().toArray(new FeatureClass[0]);
		}
		for (int i=0;i<featureArray.length;i++) {
			featureArray[i].processSequence(r);
		}
	}	

	
}
