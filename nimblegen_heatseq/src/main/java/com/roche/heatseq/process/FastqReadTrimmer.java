package com.roche.heatseq.process;

import java.io.File;
import java.io.IOException;

import net.sf.picard.fastq.FastqRecord;
import net.sf.picard.fastq.FastqWriter;
import net.sf.picard.fastq.FastqWriterFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.cli.CliStatusConsole;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ProbesBySequenceName;
import com.roche.heatseq.utils.FastqReader;
import com.roche.heatseq.utils.ProbeFileUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class FastqReadTrimmer {

	private static Logger logger = LoggerFactory.getLogger(FastqReadTrimmer.class);

	public static void trimReads(File inputFastqOneFile, File inputFastqTwoFile, File probeInfoFile, int extensionUidLength, int ligationUidLength, File outputFastqOneFile, File outputFastqTwoFile)
			throws IOException {
		ProbesBySequenceName probes = ProbeFileUtil.parseProbeInfoFile(probeInfoFile);

		ProbeInfoStats probeInfoStats = collectStatsFromProbeInformation(probes);

		logger.info(probeInfoStats.toString());

		int readOneTrimFromStart = extensionUidLength + probeInfoStats.getMaxExtensionPrimerLength();
		int readOneTrimStop = probeInfoStats.getMinCaptureTargetLength() + probeInfoStats.getMinLigationPrimerLength();
		int readTwoTrimFromStart = ligationUidLength + probeInfoStats.getMaxLigationPrimerLength();
		int readTwoTrimStop = probeInfoStats.getMinCaptureTargetLength() + probeInfoStats.getMinExtensionPrimerLength();

		logger.info("read one--first base to keep:" + readOneTrimFromStart + "  lastBaseToKeep:" + readOneTrimStop);
		logger.info("read two--first base to keep:" + readTwoTrimFromStart + "  lastBaseToKeep:" + readTwoTrimStop);

		trimReads(inputFastqOneFile, outputFastqOneFile, readOneTrimFromStart, readOneTrimStop);
		CliStatusConsole.logStatus("Finished trimming (1 of 2): " + inputFastqOneFile.getAbsolutePath() + ".  The trimmed output has been placed at " + outputFastqOneFile.getAbsolutePath() + "."
				+ StringUtil.NEWLINE);
		trimReads(inputFastqTwoFile, outputFastqTwoFile, readTwoTrimFromStart, readTwoTrimStop);
		CliStatusConsole.logStatus("Finished trimming (2 of 2):" + inputFastqTwoFile.getAbsolutePath() + ".  The trimmed output has been placed at " + outputFastqTwoFile.getAbsolutePath() + "."
				+ StringUtil.NEWLINE);
	}

	static ProbeInfoStats collectStatsFromProbeInformation(ProbesBySequenceName probes) throws IOException {

		int maxExtensionPrimerLength = 0;
		int maxLigationPrimerLength = 0;
		int maxCaptureTargetLength = 0;

		int minExtensionPrimerLength = Integer.MAX_VALUE;
		int minLigationPrimerLength = Integer.MAX_VALUE;
		int minCaptureTargetLength = Integer.MAX_VALUE;

		for (Probe probe : probes) {
			int extensionPrimerLength = probe.getExtensionPrimerSequence().size();
			int ligationPrimerLength = probe.getLigationPrimerSequence().size();
			int captureTargetLength = probe.getCaptureTargetSequence().size();
			maxExtensionPrimerLength = Math.max(extensionPrimerLength, maxExtensionPrimerLength);
			maxLigationPrimerLength = Math.max(ligationPrimerLength, maxLigationPrimerLength);
			maxCaptureTargetLength = Math.max(captureTargetLength, maxCaptureTargetLength);
			minExtensionPrimerLength = Math.min(extensionPrimerLength, minExtensionPrimerLength);
			minLigationPrimerLength = Math.min(ligationPrimerLength, minLigationPrimerLength);
			minCaptureTargetLength = Math.min(captureTargetLength, minCaptureTargetLength);
		}

		return new ProbeInfoStats(maxExtensionPrimerLength, maxLigationPrimerLength, maxCaptureTargetLength, minExtensionPrimerLength, minLigationPrimerLength, minCaptureTargetLength);
	}

	static class ProbeInfoStats {
		private final int maxExtensionPrimerLength;
		private final int maxLigationPrimerLength;
		private final int maxCaptureTargetLength;

		private final int minExtensionPrimerLength;
		private final int minLigationPrimerLength;
		private final int minCaptureTargetLength;

		public ProbeInfoStats(int maxExtensionPrimerLength, int maxLigationPrimerLength, int maxCaptureTargetLength, int minExtensionPrimerLength, int minLigationPrimerLength,
				int minCaptureTargetLength) {
			super();
			this.maxExtensionPrimerLength = maxExtensionPrimerLength;
			this.maxLigationPrimerLength = maxLigationPrimerLength;
			this.maxCaptureTargetLength = maxCaptureTargetLength;
			this.minExtensionPrimerLength = minExtensionPrimerLength;
			this.minLigationPrimerLength = minLigationPrimerLength;
			this.minCaptureTargetLength = minCaptureTargetLength;
		}

		public int getMaxExtensionPrimerLength() {
			return maxExtensionPrimerLength;
		}

		public int getMaxLigationPrimerLength() {
			return maxLigationPrimerLength;
		}

		public int getMaxCaptureTargetLength() {
			return maxCaptureTargetLength;
		}

		public int getMinExtensionPrimerLength() {
			return minExtensionPrimerLength;
		}

		public int getMinLigationPrimerLength() {
			return minLigationPrimerLength;
		}

		public int getMinCaptureTargetLength() {
			return minCaptureTargetLength;
		}

		@Override
		public String toString() {
			return "ProbeInfoStats [maxExtensionPrimerLength=" + maxExtensionPrimerLength + ", maxLigationPrimerLength=" + maxLigationPrimerLength + ", maxCaptureTargetLength="
					+ maxCaptureTargetLength + ", minExtensionPrimerLength=" + minExtensionPrimerLength + ", minLigationPrimerLength=" + minLigationPrimerLength + ", minCaptureTargetLength="
					+ minCaptureTargetLength + "]";
		}

	}

	public static void trimReads(File inputFastqFile, File outputFastqFile, int firstBaseToKeep, int lastBaseToKeep) {
		if (firstBaseToKeep < 0) {
			throw new IllegalArgumentException("First base to keep[" + firstBaseToKeep + "] must be greater than zero.");
		}

		if (lastBaseToKeep <= firstBaseToKeep) {
			throw new IllegalArgumentException("Last base to keep[" + lastBaseToKeep + "] must be greater than the first base to keep[" + firstBaseToKeep + "].");
		}

		if (outputFastqFile.exists()) {
			outputFastqFile.delete();
		}
		try {
			FileUtil.createNewFile(outputFastqFile);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create an output file at [" + outputFastqFile.getAbsolutePath() + "].", e);
		}

		FastqWriterFactory factory = new FastqWriterFactory();
		FastqWriter fastQWriter = factory.newWriter(outputFastqFile);
		try {
			try (FastqReader fastQReader = new FastqReader(inputFastqFile)) {
				while (fastQReader.hasNext()) {
					FastqRecord record = fastQReader.next();
					FastqRecord newRecord = trim(record, firstBaseToKeep, lastBaseToKeep);
					fastQWriter.write(newRecord);
				}
			}
		} finally {
			fastQWriter.close();
		}
	}

	static FastqRecord trim(FastqRecord record, int firstBaseToKeep, int lastBaseToKeep) {
		String readString = record.getReadString();
		String readQuality = record.getBaseQualityString();

		if (firstBaseToKeep >= readString.length()) {
			throw new IllegalArgumentException("Unable to trim " + firstBaseToKeep + " bases from the beginning of a sequence with length[" + readString.length() + "]");
		}

		int lastBase = Math.min(lastBaseToKeep, readString.length() - 1);

		String newReadString = readString.substring(firstBaseToKeep, lastBase + 1);
		String newReadQuality = readQuality.substring(firstBaseToKeep, lastBase + 1);

		FastqRecord newRecord = new FastqRecord(record.getReadHeader(), newReadString, record.getBaseQualityHeader(), newReadQuality);
		return newRecord;
	}
}
