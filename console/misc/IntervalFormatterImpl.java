package wbs.platform.console.misc;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wbs.framework.application.annotations.SingletonComponent;

@SingletonComponent ("intervalFormatter")
public
class IntervalFormatterImpl
	implements IntervalFormatter {

	@Override
	public
	Integer processIntervalStringSeconds (
			String input) {

		for (IntervalMatcher intervalMatcher
				: intervalMatchers) {

			Integer interval =
				intervalMatcher.match (input);

			if (interval == null)
				continue;

			return interval;

		}

		return null;

	}

	@Override
	public
	String createIntervalStringSeconds (
			int input) {

		for (IntervalMatcher intervalMatcher
				: intervalMatchers) {

			String intervalString =
				intervalMatcher.textify (input);

			if (intervalString == null)
				continue;

			return intervalString;

		}

		throw new RuntimeException ();

	}

	private static
	interface IntervalMatcher {

		Integer match (
				String input);

		String textify (
				int input);

	}

	private static
	class SimpleIntervalMatcher
		implements IntervalMatcher {

		String singularLabel;
		String pluralLabel;
		Pattern pattern;
		int scale;

		SimpleIntervalMatcher (
				int newScale,
				String newSingularLabel,
				String newPluralLabel,
				String... otherLabels) {

			scale =
				newScale;

			singularLabel =
				newSingularLabel;

			pluralLabel =
				newPluralLabel;

			StringBuilder stringBuilder =
				new StringBuilder ();

			stringBuilder.append ("\\s*(\\d+)\\s*(");
			stringBuilder.append (Pattern.quote (singularLabel));

			if (pluralLabel != null && ! singularLabel.equals (pluralLabel)) {
				stringBuilder.append ('|');
				stringBuilder.append (Pattern.quote (pluralLabel));
			}

			for (String otherLabel : otherLabels) {
				stringBuilder.append ('|');
				stringBuilder.append (Pattern.quote (otherLabel));
			}

			stringBuilder.append (")\\s*");

			pattern =
				Pattern.compile (stringBuilder.toString ());

		}

		@Override
		public
		Integer match (
				String input) {

			Matcher matcher =
				pattern.matcher (input);

			return matcher.matches ()
				? Integer.parseInt (matcher.group (1)) * scale
				: null;

		}

		@Override
		public
		String textify (
				int input) {

			if (input % scale != 0)
				return null;

			if (input / scale == 1)
				return "1 " + singularLabel;

			return stringFormat (
				"%s %s",
				input / scale,
				pluralLabel);

		}

	}

	Pattern zeroPattern =
		Pattern.compile ("\\s*0+\\s*");

	IntervalMatcher[] intervalMatchers =
		new IntervalMatcher [] {

		new IntervalMatcher () {

			@Override
			public
			Integer match (
					String input) {

				return zeroPattern.matcher (input).matches ()
					? 0
					: null;

			}

			@Override
			public
			String textify (
					int input) {

				return input == 0 ? "0" : null;

			}

		},

		new SimpleIntervalMatcher (
			31556736,
			"year",
			"years"),

		new SimpleIntervalMatcher (
			2629728,
			"month",
			"months"),

		new SimpleIntervalMatcher (
			86400,
			"day",
			"days",
			"d"),

		new SimpleIntervalMatcher (
			3600,
			"hour",
			"hours",
			"h"),

		new SimpleIntervalMatcher (
			60,
			"minute",
			"minutes",
			"mins",
			"min",
			"m"),

		new SimpleIntervalMatcher (
			1,
			"second",
			"seconds",
			"secs",
			"sec",
			"s")

	};

}
