/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.sequencing.bioinformatics.common.alignment;

import com.roche.sequencing.bioinformatics.common.sequence.ICode;

public class SimpleAlignmentScorer implements IAlignmentScorer {
	private final int match;
	private final int mismatch;
	private final int gapExtension;
	private final int gapStart;
	private final boolean shouldPenalizeTerminalGaps;

	public SimpleAlignmentScorer() {
		super();
		this.match = 1;
		this.mismatch = -1;
		this.gapExtension = -1;
		this.gapStart = -5;
		shouldPenalizeTerminalGaps = false;
	}

	public SimpleAlignmentScorer(int match, int mismatch, int gapExtension, int gapStart, boolean shouldPenalizeTerminalGaps) {
		super();
		this.match = match;
		this.mismatch = mismatch;
		this.gapExtension = gapExtension;
		this.gapStart = gapStart;
		this.shouldPenalizeTerminalGaps = shouldPenalizeTerminalGaps;
	}

	@Override
	public int getMatchScore(ICode codeOne, ICode codeTwo) {
		int score = 0;

		if (codeOne.matches(codeTwo)) {
			score += match;
		} else {
			score += mismatch;
		}

		return score;
	}

	@Override
	public int getGapScore() {
		return gapExtension;
	}

	@Override
	public int getGapStartScore() {
		return gapStart;
	}

	@Override
	public boolean shouldPenalizeTerminalGaps() {
		return shouldPenalizeTerminalGaps;
	}

}