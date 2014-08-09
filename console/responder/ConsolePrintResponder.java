package wbs.platform.console.responder;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;

import wbs.framework.utils.etc.StringFormatter;
import wbs.platform.console.request.ConsoleRequestContext;

public abstract
class ConsolePrintResponder
	extends ConsoleResponder {

	@Inject
	protected
	ConsoleRequestContext requestContext;

	protected
	PrintWriter out;

	@Override
	protected
	void setup ()
		throws IOException {

		out =
			requestContext.writer ();

	}

	protected
	void printFormat (
			Object... args) {

		out.print (
			StringFormatter.standard (
				args));

	}

}
