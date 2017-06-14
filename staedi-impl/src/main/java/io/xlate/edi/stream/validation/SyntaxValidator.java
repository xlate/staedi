package io.xlate.edi.stream.validation;

import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.stream.EDIStreamConstants.ElementOccurrenceErrors;
import io.xlate.edi.stream.EDIStreamConstants.Events;
import io.xlate.edi.stream.Location;
import io.xlate.edi.stream.internal.EventHandler;

import java.util.List;

abstract class SyntaxValidator {

	static SyntaxValidator getInstance(int type) {
		switch (type) {
		case EDISyntaxRule.SYNTAX_CONDITIONAL:
			return ConditionSyntaxValidator.getInstance();
		case EDISyntaxRule.SYNTAX_EXCLUSION:
			return ExclusionSyntaxValidator.getInstance();
		case EDISyntaxRule.SYNTAX_LIST:
			return ListSyntaxValidator.getInstance();
		case EDISyntaxRule.SYNTAX_PAIRED:
			return PairedSyntaxValidator.getInstance();
		case EDISyntaxRule.SYNTAX_REQUIRED:
			return RequiredSyntaxValidator.getInstance();
		default:
			throw new IllegalArgumentException(
					"Unexpected syntax restriction type " + type + ".");
		}
	}

	protected class SyntaxStatus {
		protected int elementCount = 0;
		protected boolean anchorPresent = false;
	}

	abstract void validate(
			EDISyntaxRule syntax,
			Location location,
			List<UsageNode> children,
			EventHandler handler);

	protected SyntaxStatus scanSyntax(
			EDISyntaxRule syntax,
			List<UsageNode> children) {

		SyntaxStatus status = new SyntaxStatus();
		boolean anchorPosition = true;

		for (int pos : syntax.getPositions()) {
			if (pos < children.size()) {
				UsageNode node = children.get(pos - 1);

				if (node.isUsed()) {
					status.elementCount++;

					if (anchorPosition) {
						status.anchorPresent = true;
					}
				}

				anchorPosition = false;
			} else {
				break;
			}
		}

		return status;
	}

	protected static void signalConditionError(
			EDISyntaxRule syntax,
			Location location,
			List<UsageNode> children,
			EventHandler handler) {

		for (int pos : syntax.getPositions()) {
			if (pos < children.size()) {
				UsageNode node = children.get(pos - 1);

				if (!node.isUsed()) {
					final int element;
					int component = location.getComponentPosition();

					if (component > -1) {
						element = location.getElementPosition();
						component = pos;
					} else {
						element = pos;
					}

					handler.elementError(
							Events.ELEMENT_OCCURRENCE_ERROR,
							ElementOccurrenceErrors.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING,
							element,
							component,
							location.getElementOccurrence());
				}
			} else {
				break;
			}
		}
	}

	protected static void signalExclusionError(
			EDISyntaxRule syntax,
			Location location,
			List<UsageNode> children,
			EventHandler handler) {

		int tally = 0;

		for (int pos : syntax.getPositions()) {
			if (pos < children.size()) {
				UsageNode node = children.get(pos - 1);

				if (node.isUsed() && ++tally > 1) {
					final int element;
					int component = location.getComponentPosition();

					if (component > -1) {
						element = location.getElementPosition();
						component = pos - 1;
					} else {
						element = pos - 1;
					}

					handler.elementError(
							Events.ELEMENT_OCCURRENCE_ERROR,
							ElementOccurrenceErrors.EXCLUSION_CONDITION_VIOLATED,
							element,
							component,
							location.getElementOccurrence());
				}
			} else {
				break;
			}
		}
	}
}
