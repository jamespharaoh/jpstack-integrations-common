package wbs.platform.exception.console;

import static wbs.web.utils.HtmlAttributeUtils.htmlIdAttribute;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import lombok.NonNull;

import wbs.console.context.ConsoleApplicationScriptRef;
import wbs.console.html.HtmlLink;
import wbs.console.html.ScriptRef;
import wbs.console.part.AbstractPagePart;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.logging.TaskLogger;

@PrototypeComponent ("exceptionStatusLinePart")
public
class ExceptionStatusLinePart
	extends AbstractPagePart {

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef> of (

			ConsoleApplicationScriptRef.javascript (
				"/js/exception-status.js")

		);

	}

	@Override
	public
	Set <HtmlLink> links () {

		return ImmutableSet.<HtmlLink> of (

			HtmlLink.applicationCssStyle (
				"/style/exception-status.css")

		);

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull TaskLogger parentTaskLogger) {

		htmlTableRowOpen (
			htmlIdAttribute (
				"exceptionsRow"));

		htmlTableCellWrite (
			"—",
			htmlIdAttribute (
				"exceptionsCell"));

		htmlTableRowClose ();

	}

}
