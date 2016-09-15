package wbs.utils.string;

import static wbs.utils.web.HtmlUtils.htmlEncode;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

import wbs.utils.web.HtmlUtils;

public
class StringFormatter {

	private final
	Map<Character,Conversion> conversions;

	public
	StringFormatter (
			@NonNull Map<Character,Conversion> newConversions) {

		conversions =
			newConversions;

	}

	/**
	 * Finds a series of formats and their args and calls formatReal () with
	 * them, returning the resulting strings in a list.
	 */
	public
	List <String> formatSpecial (
			@NonNull List <Object> arguments) {

		List <String> stringsToReturn =
			new ArrayList<> ();

		for (
			int argumentIndex = 0;
			argumentIndex < arguments.size ();
			argumentIndex ++
		) {

			String format =
				(String)
				arguments.get (
					argumentIndex);

			/*
			if (
				referenceNotEqualSafe (
					format,
					format.intern ())
			) {

				throw new IllegalArgumentException (
					stringFormat (
						"Format string at position %s ",
						argumentIndex,
						"is not interned, so probably a bug"));

			}
			*/

			int numPercents =
				numPercents (format);

			stringsToReturn.add (
				formatReal (
					format,
					arguments.subList (
						argumentIndex + 1,
						argumentIndex + 1 + numPercents)));

			argumentIndex +=
				numPercents;

		}

		return stringsToReturn;

	}

	public
	String formatReal (
			@NonNull String format,
			@NonNull List<?> argumenta) {

		StringBuilder stringBuilder =
			new StringBuilder ();

		int searchPosition = 0;

		Iterator<?> iterator =
			argumenta.iterator ();

		for (;;) {

			// find the next %

			int percentPosition =
				format.indexOf (
					'%',
					searchPosition);

			if (percentPosition < 0) {

				stringBuilder.append (
					format,
					searchPosition,
					format.length ());

				return stringBuilder.toString ();

			}

			// append the text leading up to the % bit

			stringBuilder.append (
				format,
				searchPosition,
				percentPosition);

			// get the next character

			if (percentPosition + 2 > format.length ()) {

				throw new RuntimeException (
					"Invalid format string - single % at end");

			}

			char formatCharacter =
				format.charAt (
					percentPosition + 1);

			// a double % inserts a single %

			if (formatCharacter == '%') {

				stringBuilder.append (
					'%');

				searchPosition =
					percentPosition + 2;

				continue;

			}

			// lookup the conversion

			Conversion conversion =
				conversions.get (
					formatCharacter);

			if (conversion == null) {

				throw new RuntimeException (
					"Invalid format char: " + formatCharacter);

			}

			// append the converted string

			stringBuilder.append (
				conversion.convert (
					iterator.next ()));

			searchPosition =
				percentPosition + 2;

		}

	}

	public
	String format (
			@NonNull Object... arguments) {

		return formatArray (
			arguments);

	}

	public
	String formatArray (
			@NonNull Object[] arguments) {

		StringBuilder stringBuilder =
			new StringBuilder ();

		for (
			String string
				: formatSpecial (
					Arrays.asList (arguments))
		) {

			stringBuilder.append (
				string);

		}

		return stringBuilder.toString ();

	}

	public static
	int numPercents (
			@NonNull String format) {

		int position = 0;
		int count = 0;

		for (;;) {

			position =
				format.indexOf (
					'%',
					position);

			if (position < 0)
				return count;

			if (position + 2 > format.length ()) {

				throw new RuntimeException (
					"Lone % at end of format string");

			}

			if (format.charAt (position + 1) != '%') {

				count ++;

			}

			position += 2;

		}

	}

	static
	interface Conversion {

		String convert (
			Object arg);

	}

	public static
	class StringConversion
		implements Conversion {

		@Override
		public
		String convert (
				@NonNull Object argument) {

			return argument.toString ();

		}

	}

	public static
	class HtmlConversion
		implements Conversion {

		@Override
		public
		String convert (
				@NonNull Object argument) {

			return htmlEncode (
				argument.toString ());

		}

	}

	public static
	class JavaScriptConversion
		implements Conversion {

		@Override
		public
		String convert (
				@NonNull Object argument) {

			return HtmlUtils.javascriptStringEscape (
				argument.toString ());

		}

	}

	public static
	class UrlConversion
		implements Conversion {

		@Override
		public
		String convert (
				@NonNull Object argument) {

			return HtmlUtils.urlQueryParameterEncode (
				argument.toString ());

		}

	}

	public static
	class DecimalConversion
		implements Conversion {

		@Override
		public
		String convert (
				@NonNull Object argument) {

			Number number =
				(Number)
				argument;

			return HtmlUtils.htmlEncode (
				number.toString ());

		}

	}

	private final static
	Map<Character,Conversion> standardConversions =
		ImmutableMap.<Character,Conversion>builder ()

		.put ('d', new DecimalConversion ())
		.put ('s', new StringConversion ())
		.put ('h', new HtmlConversion ())
		.put ('j', new JavaScriptConversion ())
		.put ('u', new UrlConversion ())

		.build ();

	public final static
	StringFormatter standardStringFormatter =
		new StringFormatter (
			standardConversions);

	public static
	String standard (
			@NonNull Object... arguments) {

		return standardStringFormatter.formatArray (
			arguments);

	}

	public static
	String standardArray (
			@NonNull Object[] arguments) {

		return standardStringFormatter.formatArray (
			arguments);

	}

	public static
	void printWriterFormat (
			PrintWriter printWriter,
			Object... arguments) {

		printWriter.print (
			standardArray (
				arguments));

	}

}
