package wbs.platform.console.helper;

import wbs.framework.record.Record;

public
interface ConsoleObjectManagerMethods {

	ConsoleHelper<?> getConsoleObjectHelper (
			Record<?> dataObject);

	ConsoleHelper<?> getConsoleObjectHelper (
			Class<?> objectClass);

	String tdForObject (
			Record<?> object,
			Record<?> assumedRoot,
			boolean mini,
			boolean link,
			int colspan);

	String tdForObjectMiniLink (
			Record<?> object);

	String tdForObjectMiniLink (
			Record<?> object,
			Record<?> assumedRoot);

	String tdForObjectMiniLink (
			Record<?> object,
			int colspan);

	String tdForObjectMiniLink (
			Record<?> object,
			Record<?> assumedRoot,
			int colspan);

	String tdForObjectLink (
			Record<?> object);

	String tdForObjectLink (
			Record<?> object,
			Record<?> assumedRoot);

	String tdForObjectLink (
			Record<?> object,
			int colspan);

	String tdForObjectLink (
			Record<?> object,
			Record<?> assumedRoot,
			int colspan);

	String htmlForObject (
			Record<?> object,
			Record<?> assumedRoot,
			boolean mini);

	boolean canView (
			Record<?> object);

	String contextName (
			Record<?> object);

	String contextLink (
			Record<?> object);

	String localLink (
			Record<?> object);

	String objectToSimpleHtml (
			Object object,
			Record<?> assumedRoot,
			boolean mini);

}