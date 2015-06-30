package test.java.uk.ac.babraham.BamQC.Modules;

import static org.junit.Assert.*;

import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import uk.ac.babraham.BamQC.Modules.InsertLengthDistribution;

public class InsertLengthDistributionTest {

	private static Logger log = Logger.getLogger(InsertLengthDistributionTest.class);

	private InsertLengthDistribution insertLengthDistribution;
	private TestObjectFactory testObjectFactory;
	private List<SAMRecord> samRecords;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {}

	@Before
	public void setUp() throws Exception {
		testObjectFactory = new TestObjectFactory();
		samRecords = testObjectFactory.getSamRecords();

		insertLengthDistribution = new InsertLengthDistribution();
	}

	@After
	public void tearDown() throws Exception {}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testProcessSequence() {
		log.info("testProcessSequence");

		for (SAMRecord samRecord : samRecords) {
			insertLengthDistribution.processSequence(samRecord);
		}
		List<Long> distribution = insertLengthDistribution.getDistribution();

		// distribution now takes account of negative values
		assertEquals(0, (long) distribution.get(0));
		assertEquals(0, (long) distribution.get(1));
		assertEquals(0, (long) distribution.get(2));
		assertEquals(1, (long) distribution.get(3));

		assertEquals(2, insertLengthDistribution.getUnpairedReads());

		assertTrue(insertLengthDistribution.raisesWarning());
		assertTrue(insertLengthDistribution.raisesError());

		// throws exception
		distribution.get(4);
	}

	@Test
	public void testProcessSequenceRaise() {
		log.info("testProcessSequenceRaise");

		samRecords.get(0).setInferredInsertSize(7);
		samRecords.get(1).setInferredInsertSize(3);
		samRecords.get(2).setInferredInsertSize(1);

		for (SAMRecord samRecord : samRecords) {
			insertLengthDistribution.processSequence(samRecord);
		}
		assertTrue(insertLengthDistribution.raisesError());
		assertTrue(insertLengthDistribution.raisesWarning());
	}

	@Test
	public void testProcessSequenceRaiseCalculation() {
		List<Long> insertSizes = UtilityTest.readInsertSizesLong();
		int index = 0;
		SAMFileHeader samFileHeader = TestObjectFactory.getInstance();
		SAMRecord samRecord = new SAMRecord(samFileHeader);

		samRecord.setProperPairFlag(true);
		samRecord.setReadPairedFlag(true);

		for (long count : insertSizes) {
			for (int i = 0; i < count; i++) {
				samRecord.setInferredInsertSize(index);
				
				insertLengthDistribution.processSequence(samRecord);
			}
			index++;
		}
		assertFalse(insertLengthDistribution.raisesError());
		assertFalse(insertLengthDistribution.raisesWarning());
	}

}