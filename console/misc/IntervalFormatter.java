package wbs.platform.console.misc;

public
interface IntervalFormatter {

	Integer processIntervalStringSeconds (
			String input);

	String createIntervalStringSeconds (
			int input);

}
