package wbs.platform.console.tab;

import static wbs.framework.utils.etc.Misc.joinWithSeparator;
import static wbs.framework.utils.etc.Misc.joinWithoutSeparator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.record.GlobalId;
import wbs.platform.console.html.HtmlLink;
import wbs.platform.console.html.ScriptRef;
import wbs.platform.console.part.PagePart;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.console.responder.HtmlResponder;
import wbs.platform.exception.logic.ExceptionLogic;
import wbs.platform.exception.logic.ExceptionLogicImpl;
import wbs.platform.priv.console.PrivChecker;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

@Accessors (fluent = true)
@PrototypeComponent ("tabbedResponder")
public
class TabbedResponder
	extends HtmlResponder {

	// dependencies

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	ExceptionLogic exceptionLogic;

	@Inject
	PrivChecker privChecker;

	// properties

	@Getter @Setter
	Tab tab;

	@Getter @Setter
	String title;

	@Getter @Setter
	PagePart pagePart;

	// state

	Throwable pagePartThrew;

	List<MyLayer> myLayers =
		new ArrayList<MyLayer> ();

	// details

	@Override
	protected
	Set<HtmlLink> getLinks () {

		return ImmutableSet.<HtmlLink>builder ()

			.addAll (
				super.getLinks ())

			.addAll (
				pagePart.links ())

			.build ();

	}

	@Override
	protected
	Set<ScriptRef> scriptRefs () {

		return pagePart.scriptRefs ();

	}

	@Override
	protected
	String getTitle () {

		return title;

	}

	// implementation

	@Override
	protected
	void setup ()
		throws IOException {

		super.setup ();

		if (pagePart != null) {

			pagePart.setup (
				Collections.<String,Object>emptyMap ());

		}

	}

	@Override
	protected
	void prepare () {

		super.prepare ();

		TabContext tabContext =
			requestContext.tabContext ();

		MyLayer myLayer1 = null;

		for (TabContext.Layer tabContextLayer
				: tabContext.getLayers ()) {

			myLayers.add (
				myLayer1 =
					new MyLayer ()

					.title (
						tabContextLayer.title ())

					.tabList (
						tabContextLayer.tabList ())

					.tab (
						tabContextLayer.tab ()));

		}

		if (myLayer1 == null)
			throw new RuntimeException ();

		myLayer1.tab (tab);

		if (pagePart != null) {

			try {

				pagePart.prepare ();

			} catch (RuntimeException exception) {

				// record the exception

				String path =
					joinWithoutSeparator (
						requestContext.servletPath (),
						requestContext.pathInfo () != null
							? requestContext.pathInfo ()
							: "");

				exceptionLogic.logThrowable (
					"console",
					path,
					exception,
					Optional.fromNullable (
						requestContext.userId ()),
					false);

				// and remember we had a problem

				pagePartThrew =
					exception;

				requestContext.addError (
					"Internal error");

			}

		}

	}

	@Override
	protected
	void goHeadStuff () {

		super.goHeadStuff ();

		pagePart.goHeadStuff ();

		printFormat (
			"<script type=\"text/javascript\">\n",

			"function toggleHead (elem) {\n",
			"  while (elem.nodeName.toLowerCase () != 'table')\n",
			"    elem = elem.parentNode;\n",
			"  if (elem.className == 'head-1-big')\n",
			"    elem.className = 'head-1-small';\n",
			"  else if (elem.className == 'head-1-small')\n",
			"    elem.className = 'head-1-big';\n",
			"}\n",

			"</script>\n");

	}

	protected
	void goTab () {
	}

	@Override
	protected
	void goBodyStuff () {

		printFormat (
			"<h1>%h</h1>\n",
			title);

		for (MyLayer myLayer : myLayers) {

			printFormat (
				"<table",
				" class=\"head-1-big\"",
				"><tr>\n",

				"<td",
				" class=\"h\"",
				" onclick=\"toggleHead (this)\"",
				">%h</td>\n",
				myLayer.title);

			printFormat (
				"<td class=\"l\">\n");

			for (TabRef tabRef
					: myLayer.tabList.getTabRefs ()) {

				if (! tabRef.getTab ().isAvailable ())
					continue;

				if (tabRef.getTab () == myLayer.tab) {

					printFormat (
						"<a",
						" class=\"selected\"",
						" href=\"%h\"",
						tabRef.getTab ().getUrl (),
						">%h</a>\n",
						tabRef.getLabel ());

				} else {

					printFormat (
						"<a",
						" href=\"%h\"",
						tabRef.getTab ().getUrl (),
						">%h</a>\n",
						tabRef.getLabel ());

				}

			}

			printFormat (
				"</td>\n",

				"</tr></table>\n");

		}

		printFormat (
			"<div style=\"%h\"></div>\n",
			joinWithSeparator (
				"; ",
				"clear: both",
				"border-top: 1px solid white",
				"margin-bottom: 1ex"));

		requestContext.flushNotices (out);

		if (pagePartThrew != null) {

			printFormat (
				"<p>Unable to show page contents.</p>\n");

			if (privChecker.can (
					GlobalId.root,
					"debug")) {

				printFormat (
					"<p><pre>%h</pre></p>\n",
					ExceptionLogicImpl.throwableDump (pagePartThrew));

			}

		} else if (pagePart != null) {

			pagePart.goBodyStuff ();

		}

	}

	@Accessors (fluent = true)
	@Data
	private static
	class MyLayer {
		String title;
		TabList tabList;
		Tab tab;
	}

}
