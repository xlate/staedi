package io.xlate.edi.stream.validation;

import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.stream.Location;
import io.xlate.edi.stream.internal.EventHandler;

import java.util.List;

class ListSyntaxValidator extends SyntaxValidator {

	private static final ListSyntaxValidator singleton =
			new ListSyntaxValidator();

	private ListSyntaxValidator() {}

	static ListSyntaxValidator getInstance() {
		return singleton;
	}

	@Override
	void validate(
			EDISyntaxRule syntax,
			Location location,
			List<UsageNode> children,
			EventHandler handler) {

		SyntaxStatus status = super.scanSyntax(syntax, children);

		if (status.anchorPresent && status.elementCount == 1) {
			signalConditionError(syntax, location, children, handler);
		}
	}
}
