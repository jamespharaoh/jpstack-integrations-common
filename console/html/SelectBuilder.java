package wbs.platform.console.html;

import static wbs.framework.utils.etc.Misc.equal;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;

@Accessors (fluent = true)
@PrototypeComponent ("selectBuilder")
public
class SelectBuilder {

	@Getter @Setter
	String id;

	@Getter @Setter
	String htmlClass;

	@Getter @Setter
	String selectName;

	@Getter @Setter
	String selectedValue;

	@Getter @Setter
	Map<String,String> options =
		new LinkedHashMap<String,String> ();

	public
	String build () {

		StringBuilder stringBuilder =
			new StringBuilder ();

		stringBuilder.append (
			stringFormat (
				"<select"));

		if (id != null) {

			stringBuilder.append (
				stringFormat (
					" id=\"%h\"", id));

		}

		if (htmlClass != null) {

			stringBuilder.append (
				stringFormat (
					" class=\"%h\"",
					htmlClass));

		}

		if (selectName != null) {

			stringBuilder.append (
				stringFormat (
					" name=\"%h\"",
					selectName));

		}

		stringBuilder.append (
			stringFormat (
				">\n"));

		for (Map.Entry<String,String> optionEntry
				: options.entrySet ()) {

			String optionValue =
				optionEntry.getKey ();

			String optionText =
				optionEntry.getValue ();

			stringBuilder.append (
				stringFormat (
					"<option",

					" value=\"%h\"",
					optionValue,

					equal (
							optionValue,
							selectedValue)
						? " selected"
						: "",

					">%h</option>",
					optionText));

		}

		stringBuilder.append (
			stringFormat (
				"</select>\n"));

		return stringBuilder.toString ();

	}

}
