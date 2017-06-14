package io.xlate.edi.stream.validation;

import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.stream.Location;
import io.xlate.edi.stream.internal.EventHandler;

import java.util.List;

class PairedSyntaxValidator extends SyntaxValidator {

	private static final PairedSyntaxValidator singleton =
			new PairedSyntaxValidator();

	private PairedSyntaxValidator() {}

	static PairedSyntaxValidator getInstance() {
		return singleton;
	}

	@Override
	void validate(
			EDISyntaxRule syntax,
			Location location,
			List<UsageNode> children,
			EventHandler handler) {

		SyntaxStatus status = super.scanSyntax(syntax, children);

		if (status.elementCount == 0) {
			return;
		}

		if (status.elementCount < syntax.getPositions().size()) {
			signalConditionError(syntax, location, children, handler);
		}
	}
}
