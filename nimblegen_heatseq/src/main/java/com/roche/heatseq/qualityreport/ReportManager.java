package com.roche.heatseq.qualityreport;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.picard.fastq.FastqWriter;
import net.sf.picard.fastq.FastqWriterFactory;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;

import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.process.PrefuppCli;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.TabDelimitedFileWriter;

public class ReportManager {

	private final static String DUPLICATE_MAPPINGS_REPORT_NAME = "duplicate_mappings.txt";

	public final static String PROBE_DETAILS_REPORT_NAME = "probe_details.txt";
	public final static String SUMMARY_REPORT_NAME = PrefuppCli.APPLICATION_NAME + "_summary.txt";
	private final static String UID_COMPOSITION_REPORT_NAME = "uid_composition_by_probe.txt";
	private final static String UNABLE_TO_ALIGN_PRIMER_REPORT_NAME = "unable_to_align_primer_for_variable_length_uid.txt";
	public final static String UNABLE_TO_MAP_FASTQ_ONE_REPORT_NAME = "unable_to_map_one.fastq";
	public final static String UNABLE_TO_MAP_FASTQ_TWO_REPORT_NAME = "unable_to_map_two.fastq";
	private final static String PRIMER_ALIGNMENT_REPORT_NAME = "extension_primer_alignment.txt";
	private final static String UNIQUE_PROBE_TALLIES_REPORT_NAME = "unique_probe_tallies.txt";
	private final static String READS_MAPPED_TO_MULTIPLE_PROBES_REPORT_NAME = "reads_mapped_to_multiple_probes.txt";
	private final static String PROBE_COVERAGE_REPORT_NAME = "probe_coverage.bed";
	private final static String MAPPED_OFF_TARGET_READS_REPORT_NAME = "mapped_off_target_reads.bam";
	private final static String UNMAPPED_READS_REPORT_NAME = "unmapped_read_pairs.bam";
	private final static String PARTIALLY_MAPPED_READS_REPORT_NAME = "partially_mapped_read_pairs.bam";

	private TabDelimitedFileWriter ambiguousMappingWriter;
	private TabDelimitedFileWriter unableToAlignPrimerWriter;
	private TabDelimitedFileWriter primerAlignmentWriter;
	private TabDelimitedFileWriter uniqueProbeTalliesWriter;
	private TabDelimitedFileWriter probeCoverageWriter;
	private TabDelimitedFileWriter uidCompositionByProbeWriter;
	private TabDelimitedFileWriter readsMappedToMultipleProbesWriter;

	private SAMFileWriter mappedOffTargetReadsWriter;
	private SAMFileWriter unmappedReadsWriter;
	private SAMFileWriter partiallyMappedReadsWriter;
	private ProbeDetailsReport detailsReport;
	private SummaryReport summaryReport;

	private FastqWriter fastqOneUnableToMapWriter;
	private FastqWriter fastqTwoUnableToMapWriter;

	private final boolean shouldOutputReports;

	public ReportManager(String softwareName, String softwareVersion, File outputDirectory, String outputFilePrefix, int extensionUidLength, int ligationUidLength, SAMFileHeader samFileHeader,
			boolean shouldOutputReports) {

		this.shouldOutputReports = shouldOutputReports;

		if (shouldOutputReports) {
			File ambiguousMappingFile = new File(outputDirectory, outputFilePrefix + DUPLICATE_MAPPINGS_REPORT_NAME);
			try {
				FileUtil.createNewFile(ambiguousMappingFile);
				ambiguousMappingWriter = new TabDelimitedFileWriter(ambiguousMappingFile, new String[] { "read_name", "read_string", "sequence_name", "extension_primer_start",
						"extension_primer_stop", "capture_target_start", "capture_target_stop", "ligation_primer_start", "ligation_primer_stop", "probe_strand" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File unableToAlignPrimerFile = new File(outputDirectory, outputFilePrefix + UNABLE_TO_ALIGN_PRIMER_REPORT_NAME);
			try {
				FileUtil.createNewFile(unableToAlignPrimerFile);
				unableToAlignPrimerWriter = new TabDelimitedFileWriter(unableToAlignPrimerFile, new String[] { "sequence_name", "probe_start", "probe_stop", "extension_primer_sequence",
						"ligation_primer_sequence", "read_name", "read_one_string", "read_two_string", "extension_failed", "ligation_failed" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			final FastqWriterFactory factory = new FastqWriterFactory();

			File unableToMapFastqOneFile = new File(outputDirectory, outputFilePrefix + UNABLE_TO_MAP_FASTQ_ONE_REPORT_NAME);
			try {
				FileUtil.createNewFile(unableToMapFastqOneFile);
				fastqOneUnableToMapWriter = factory.newWriter(unableToMapFastqOneFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File unableToMapFastqTwoFile = new File(outputDirectory, outputFilePrefix + UNABLE_TO_MAP_FASTQ_TWO_REPORT_NAME);
			try {
				FileUtil.createNewFile(unableToMapFastqTwoFile);
				fastqTwoUnableToMapWriter = factory.newWriter(unableToMapFastqTwoFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File primerAlignmentFile = new File(outputDirectory, outputFilePrefix + PRIMER_ALIGNMENT_REPORT_NAME);
			try {
				FileUtil.createNewFile(primerAlignmentFile);
				primerAlignmentWriter = new TabDelimitedFileWriter(primerAlignmentFile, new String[] { "uid_length", "substituions", "insertions", "deletions", "edit_distance", "read",
						"extension_primer", "probe_sequence_name", "capture_target_start", "capture_target_stop", "probe_strand" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File uniqueProbeTalliesFile = new File(outputDirectory, outputFilePrefix + UNIQUE_PROBE_TALLIES_REPORT_NAME);
			try {
				FileUtil.createNewFile(uniqueProbeTalliesFile);
				uniqueProbeTalliesWriter = new TabDelimitedFileWriter(uniqueProbeTalliesFile, new String[0]);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File probeCoverageFile = new File(outputDirectory, outputFilePrefix + PROBE_COVERAGE_REPORT_NAME);
			try {
				FileUtil.createNewFile(probeCoverageFile);
				probeCoverageWriter = new TabDelimitedFileWriter(probeCoverageFile, new String[0]);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File readsMappedToMultipleProbesFile = new File(outputDirectory, outputFilePrefix + READS_MAPPED_TO_MULTIPLE_PROBES_REPORT_NAME);
			try {
				FileUtil.createNewFile(readsMappedToMultipleProbesFile);
				readsMappedToMultipleProbesWriter = new TabDelimitedFileWriter(readsMappedToMultipleProbesFile, new String[] { "read_name", "probe" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File uidCompositionByProbeFile = new File(outputDirectory, outputFilePrefix + UID_COMPOSITION_REPORT_NAME);
			try {
				FileUtil.createNewFile(uidCompositionByProbeFile);
				uidCompositionByProbeWriter = new TabDelimitedFileWriter(uidCompositionByProbeFile, new String[] {
						"probe_id",
						"unique_uid_nuclotide_composition" + StringUtil.TAB + "unique_uid_nuclotide_composition_by_position" + StringUtil.TAB + "weighted_uid_nuclotide_composition" + StringUtil.TAB
								+ "weighted_uid_nuclotide_composition_by_position" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			SAMFileWriterFactory samFactory = new SAMFileWriterFactory();

			File mappedOffTargetFile = new File(outputDirectory, outputFilePrefix + MAPPED_OFF_TARGET_READS_REPORT_NAME);
			try {
				FileUtil.createNewFile(mappedOffTargetFile);
				mappedOffTargetReadsWriter = samFactory.makeBAMWriter(samFileHeader, true, mappedOffTargetFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File unMappedFile = new File(outputDirectory, outputFilePrefix + UNMAPPED_READS_REPORT_NAME);
			try {
				FileUtil.createNewFile(unMappedFile);
				unmappedReadsWriter = samFactory.makeBAMWriter(samFileHeader, true, unMappedFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File partiallyMappedFile = new File(outputDirectory, outputFilePrefix + PARTIALLY_MAPPED_READS_REPORT_NAME);
			try {
				FileUtil.createNewFile(partiallyMappedFile);
				partiallyMappedReadsWriter = samFactory.makeBAMWriter(samFileHeader, true, partiallyMappedFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File detailsReportFile = new File(outputDirectory, outputFilePrefix + PROBE_DETAILS_REPORT_NAME);
			try {
				FileUtil.createNewFile(detailsReportFile);
				detailsReport = new ProbeDetailsReport(detailsReportFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File summaryReportFile = null;

			summaryReportFile = new File(outputDirectory, outputFilePrefix + SUMMARY_REPORT_NAME);
			try {
				FileUtil.createNewFile(summaryReportFile);
				summaryReport = new SummaryReport(softwareName, softwareVersion, summaryReportFile, extensionUidLength, ligationUidLength);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public boolean isReporting() {
		return shouldOutputReports;
	}

	public void close() {
		if (ambiguousMappingWriter != null) {
			ambiguousMappingWriter.close();
		}

		if (unableToAlignPrimerWriter != null) {
			unableToAlignPrimerWriter.close();
		}
		if (primerAlignmentWriter != null) {
			primerAlignmentWriter.close();
		}
		if (fastqOneUnableToMapWriter != null) {
			fastqOneUnableToMapWriter.close();
		}
		if (fastqTwoUnableToMapWriter != null) {
			fastqTwoUnableToMapWriter.close();
		}

		if (probeCoverageWriter != null) {
			probeCoverageWriter.close();
		}

		if (readsMappedToMultipleProbesWriter != null) {
			readsMappedToMultipleProbesWriter.close();
		}

		if (uidCompositionByProbeWriter != null) {
			uidCompositionByProbeWriter.close();
		}

		if (detailsReport != null) {
			detailsReport.close();
		}

		if (summaryReport != null) {
			summaryReport.close();
		}

		if (uniqueProbeTalliesWriter != null) {
			uniqueProbeTalliesWriter.close();
		}

		if (mappedOffTargetReadsWriter != null) {
			mappedOffTargetReadsWriter.close();
		}

		if (unmappedReadsWriter != null) {
			unmappedReadsWriter.close();
		}

		if (partiallyMappedReadsWriter != null) {
			partiallyMappedReadsWriter.close();
		}

	}

	public ProbeDetailsReport getDetailsReport() {
		return detailsReport;
	}

	public TabDelimitedFileWriter getUniqueProbeTalliesWriter() {
		return uniqueProbeTalliesWriter;
	}

	public TabDelimitedFileWriter getProbeCoverageWriter() {
		return probeCoverageWriter;
	}

	public TabDelimitedFileWriter getReadsMappedToMultipleProbesWriter() {
		return readsMappedToMultipleProbesWriter;
	}

	public TabDelimitedFileWriter getUidCompisitionByProbeWriter() {
		return uidCompositionByProbeWriter;
	}

	public SummaryReport getSummaryReport() {
		return summaryReport;
	}

	public TabDelimitedFileWriter getUnableToAlignPrimerWriter() {
		return unableToAlignPrimerWriter;
	}

	public TabDelimitedFileWriter getAmbiguousMappingWriter() {
		return ambiguousMappingWriter;
	}

	public FastqWriter getFastqOneUnableToMapWriter() {
		return fastqOneUnableToMapWriter;
	}

	public FastqWriter getFastqTwoUnableToMapWriter() {
		return fastqTwoUnableToMapWriter;
	}

	public TabDelimitedFileWriter getPrimerAlignmentWriter() {
		return primerAlignmentWriter;
	}

	public SAMFileWriter getMappedOffTargetReadsWriter() {
		return mappedOffTargetReadsWriter;
	}

	public SAMFileWriter getUnMappedReadPairsWriter() {
		return unmappedReadsWriter;
	}

	public SAMFileWriter getPartiallyMappedReadPairsWriter() {
		return partiallyMappedReadsWriter;
	}

	public void completeSummaryReport(Map<String, Set<Probe>> readNamesToDistinctProbeAssignmentCount, Set<ISequence> distinctUids, List<ISequence> nonDistinctUids, long processingTimeInMs,
			int totalProbes, int totalReads, int totalFullyMappedOffTargetReads, int totalPartiallyMappedReads, int totalFullyUnmappedReads, int totalFullyMappedOnTargetReads) {
		summaryReport.setProcessingTimeInMs(processingTimeInMs);
		summaryReport.setDuplicateReadPairsRemoved(detailsReport.getDuplicateReadPairsRemoved());
		summaryReport.setProbesWithNoMappedReadPairs(detailsReport.getProbesWithNoMappedReadPairs());
		summaryReport.setTotalReadPairsAfterReduction(detailsReport.getTotalReadPairsAfterReduction());

		summaryReport.setAverageUidsPerProbe(detailsReport.getAverageNumberOfUidsPerProbe());
		summaryReport.setAverageUidsPerProbeWithReads(detailsReport.getAverageNumberOfUidsPerProbeWithAssignedReads());
		summaryReport.setMaxUidsPerProbe(detailsReport.getMaxNumberOfUidsPerProbe());
		summaryReport.setAverageNumberOfReadPairsPerProbeUid(detailsReport.getAverageNumberOfReadPairsPerProbeUid());

		int readPairsAssignedToMultipleProbes = 0;
		for (Set<Probe> probes : readNamesToDistinctProbeAssignmentCount.values()) {
			if (probes.size() > 1) {
				readPairsAssignedToMultipleProbes++;
			}
		}
		summaryReport.setReadPairsAssignedToMultipleProbes(readPairsAssignedToMultipleProbes);
		summaryReport.setDistinctUidsFound(distinctUids.size());
		summaryReport.setTotalProbes(totalProbes);

		summaryReport.setTotalFullyMappedOffTargetReads(totalFullyMappedOffTargetReads);

		summaryReport.setTotalPartiallyMappedReads(totalPartiallyMappedReads);

		summaryReport.setTotalFullyUnmappedReads(totalFullyUnmappedReads);

		summaryReport.setTotalFullyMappedOnTargetReads(totalFullyMappedOnTargetReads);

		summaryReport.setTotalReads(totalReads);
	}

}
