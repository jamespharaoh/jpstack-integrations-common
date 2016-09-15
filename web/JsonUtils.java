package wbs.utils.web;

import static wbs.utils.string.StringUtils.stringToUtf8;

import java.io.StringWriter;

import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.Accessors;

import org.json.simple.JSONValue;

import wbs.utils.string.FormatWriter;
import wbs.utils.string.WriterFormatWriter;
import wbs.utils.web.JsonUtils.EscapeOptions.EscapeOptionsBuilder;

public
class JsonUtils {

	public static
	byte[] jsonToBytes (
			@NonNull Object object) {

		return stringToUtf8 (
			JSONValue.toJSONString (
				object));

	}

	public static
	String escapeJsonString (
			@NonNull EscapeOptions escapeOptions,
			@NonNull String source) {

		StringWriter stringWriter =
			new StringWriter ();

		FormatWriter formatWriter =
			new WriterFormatWriter (
				stringWriter);

		writeEscapedJsonString (
			formatWriter,
			escapeOptions,
			source);

		return stringWriter.toString ();

	}

	public static
	void writeEscapedJsonString (
			@NonNull FormatWriter formatWriter,
			@NonNull EscapeOptions escapeOptions,
			@NonNull String source) {

		source.chars ().forEach (
			character -> {

			switch (character) {

			case '"':

				formatWriter.writeString (
					escapeOptions.doubleQuote);

				break;

			case '\'':

				formatWriter.writeString (
					escapeOptions.singleQuote);

				break;

			case '\n':

				formatWriter.writeString (
					"\\n");

				break;

			case '\r':

				formatWriter.writeString (
					"\\r");

				break;

			case '/':

				formatWriter.writeString (
					escapeOptions.forwardSlash);

				break;

			case '\\':

				formatWriter.writeString (
					"\\\\");

				break;

			case '<':

				formatWriter.writeString (
					escapeOptions.lessThan);

				break;

			case '>':

				formatWriter.writeString (
					escapeOptions.greaterThan);

				break;

			case '&':

				formatWriter.writeString (
					escapeOptions.ampersand);

				break;

			default:

				formatWriter.writeCharacter (
					character);

			}

		});

	}

	@Accessors (fluent = true)
	@Builder
	static
	class EscapeOptions {

		String singleQuote;
		String doubleQuote;

		String forwardSlash;

		String lessThan;
		String greaterThan;
		String ampersand;

	}

	// default options

	public final static
	EscapeOptions defaultOptions =
		new EscapeOptionsBuilder ()
			.singleQuote ("\\'")
			.doubleQuote ("\\\"")
			.forwardSlash ("/")
			.lessThan ("<")
			.greaterThan (">")
			.ampersand ("&")
			.build ();

	public final static
	EscapeOptions defaultDoubleQuotesOptions =
		new EscapeOptionsBuilder ()
			.singleQuote ("'")
			.doubleQuote ("\\\"")
			.forwardSlash ("/")
			.lessThan ("<")
			.greaterThan (">")
			.ampersand ("&")
			.build ();

	public final static
	EscapeOptions defaultSingleQuotesOptions =
		new EscapeOptionsBuilder ()
			.singleQuote ("\\'")
			.doubleQuote ("\"")
			.forwardSlash ("/")
			.lessThan ("<")
			.greaterThan (">")
			.ampersand ("&")
			.build ();

	// html options

	public final static
	EscapeOptions htmlDefaultOptions =
		new EscapeOptionsBuilder ()
			.singleQuote ("\\'")
			.doubleQuote ("\\\"")
			.forwardSlash ("/")
			.lessThan ("&lt;")
			.greaterThan ("&gt;")
			.ampersand ("&amp;")
			.build ();

	public final static
	EscapeOptions htmlDoubleQuotesOptions =
		new EscapeOptionsBuilder ()
			.singleQuote ("'")
			.doubleQuote ("\\\"")
			.forwardSlash ("/")
			.lessThan ("&lt;")
			.greaterThan ("&gt;")
			.ampersand ("&amp;")
			.build ();

	public final static
	EscapeOptions htmlSingleQuotesOptions =
		new EscapeOptionsBuilder ()
			.singleQuote ("\\'")
			.doubleQuote ("\"")
			.forwardSlash ("/")
			.lessThan ("&lt;")
			.greaterThan ("&gt;")
			.ampersand ("&amp;")
			.build ();

	// html script options

	public final static
	EscapeOptions htmlScriptDefaultOptions =
		new EscapeOptionsBuilder ()
			.singleQuote ("\\'")
			.doubleQuote ("\\\"")
			.forwardSlash ("\\/")
			.lessThan ("<")
			.greaterThan (">")
			.ampersand ("&")
			.build ();

	public final static
	EscapeOptions htmlScriptDoubleQuotesOptions =
		new EscapeOptionsBuilder ()
			.singleQuote ("'")
			.doubleQuote ("\\\"")
			.forwardSlash ("\\/")
			.lessThan ("<")
			.greaterThan (">")
			.ampersand ("&")
			.build ();

	public final static
	EscapeOptions htmlScriptSingleQuotesOptions =
		new EscapeOptionsBuilder ()
			.singleQuote ("\\'")
			.doubleQuote ("\"")
			.forwardSlash ("\\/")
			.lessThan ("<")
			.greaterThan (">")
			.ampersand ("&")
			.build ();

}
