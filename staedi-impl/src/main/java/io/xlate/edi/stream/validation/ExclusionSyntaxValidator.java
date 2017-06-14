package io.xlate.edi.stream.validation;

import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.stream.Location;
import io.xlate.edi.stream.internal.EventHandler;

import java.util.List;

class ExclusionSyntaxValidator extends SyntaxValidator {

	private static final ExclusionSyntaxValidator singleton =
			new ExclusionSyntaxValidator();

	private ExclusionSyntaxValidator() {}

	static ExclusionSyntaxValidator getInstance() {
		return singleton;
	}

	@Override
	void validate(
			EDISyntaxRule syntax,
			Location location,
			List<UsageNode> children,
			EventHandler handler) {

		SyntaxStatus status = super.scanSyntax(syntax, children);

		if (status.elementCount > 1) {
			signalExclusionError(syntax, location, children, handler);
		}
	}
}
