package wbs.platform.media.console;

import static wbs.framework.utils.etc.Misc.isPresent;
import static wbs.framework.utils.etc.Misc.moreThanZero;
import static wbs.framework.utils.etc.Misc.stringFormat;
import static wbs.framework.utils.etc.Misc.successResult;

import java.io.IOException;

import javax.inject.Inject;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;

import com.google.common.base.Optional;

import fj.data.Either;

import wbs.console.forms.FormFieldRenderer;
import wbs.console.forms.FormFieldSubmission;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.utils.etc.FormatWriter;
import wbs.framework.utils.etc.RuntimeIoException;
import wbs.platform.media.logic.MediaLogic;
import wbs.platform.media.model.MediaRec;

@PrototypeComponent ("imageFormFieldRenderer")
@Accessors (fluent = true)
public
class ImageFormFieldRenderer<Container>
	implements FormFieldRenderer<Container,MediaRec> {

	// dependencies

	@Inject
	MediaConsoleLogic mediaConsoleLogic;

	@Inject
	MediaLogic mediaLogic;

	// properties

	@Getter @Setter
	String name;

	@Getter @Setter
	String label;

	@Getter @Setter
	Boolean nullable;

	// details

	@Getter
	boolean fileUpload = true;

	// implementation

	@Override
	public
	void renderTableCellList (
			@NonNull FormatWriter out,
			@NonNull Container container,
			@NonNull Optional<MediaRec> interfaceValue,
			boolean link,
			int colspan) {

		out.writeFormat (
			"<td",
			colspan > 1
				? stringFormat (
					" colspan=\"%h\"",
					colspan)
				: "",
			">%s</td>\n",
			interfaceToHtmlSimple (
				container,
				interfaceValue,
				link));

	}

	@Override
	public
	void renderTableCellProperties (
			@NonNull FormatWriter out,
			@NonNull Container container,
			@NonNull Optional<MediaRec> interfaceValue) {

		out.writeFormat (
			"<td>%s</td>\n",
			interfaceToHtmlComplex (
				container,
				interfaceValue));

	}

	@Override
	public
	void renderTableRow (
			@NonNull FormatWriter out,
			@NonNull Container container,
			@NonNull Optional<MediaRec> interfaceValue) {

		out.writeFormat (
			"<tr>\n",
			"<th>%h</th>\n",
			label ());

		renderTableCellProperties (
			out,
			container,
			interfaceValue);

		out.writeFormat (
			"</tr>\n");

	}

	@Override
	public
	void renderFormRow (
			@NonNull FormFieldSubmission submission,
			@NonNull FormatWriter out,
			@NonNull Container container,
			@NonNull Optional<MediaRec> interfaceValue,
			@NonNull Optional<String> error) {

		out.writeFormat (
			"<tr>\n",
			"<th>%h</th>\n",
			label (),
			"<td>");

		renderFormInput (
			submission,
			out,
			container,
			interfaceValue);

		if (
			isPresent (
				error)
		) {

			out.writeFormat (
				"<br>\n",
				"%h",
				error.get ());

		}

		out.writeFormat (
			"</td>\n",
			"</tr>\n");

	}

	@Override
	public
	void renderFormInput (
			@NonNull FormFieldSubmission submission,
			@NonNull FormatWriter out,
			@NonNull Container container,
			@NonNull Optional<MediaRec> interfaceValue) {

		if (interfaceValue.isPresent ()) {

			out.writeFormat (
				"%s<br>\n",
				interfaceToHtmlComplex (
					container,
					interfaceValue));

		}

		out.writeFormat (
			"<input",
			" type=\"file\"",
			" name=\"%h\"",
			name (),
			"><br>\n");

		if (
			interfaceValue.isPresent ()
			&& nullable ()
		) {

			out.writeFormat (
				"<input",
				" type=\"submit\"",
				" name=\"%h-remove\"",
				name (),
				" value=\"remove image\"",
				">\n");

		}

	}

	@Override
	public
	void renderFormReset (
			@NonNull FormatWriter javascriptWriter,
			@NonNull String indent,
			@NonNull Container container,
			@NonNull Optional<MediaRec> interfaceValue) {

		javascriptWriter.writeFormat (
			"%s$(\"#%j\").replaceWith (\n",
			indent,
			name);

		javascriptWriter.writeFormat (
			"%s\t$(\"#%j\").clone (true));\n",
			indent,
			name);

	}

	@Override
	public
	boolean formValuePresent (
			@NonNull FormFieldSubmission submission) {

		return (

			submission.hasParameter (
				stringFormat (
					"%s-remove",
					name ()))

		) || (

			submission.multipart ()

			&& submission.hasFileItem (
				name ())

			&& moreThanZero (
				submission.fileItem (
					name ()
				).getSize ())

		);

	}

	MediaRec formValue (
			@NonNull FormFieldSubmission submission) {

		if (
			submission.hasParameter (
				stringFormat (
					"%s-remove",
					name ()))
		) {
			return null;
		}

		FileItem fileItem =
			submission.fileItem (
				name ());

		try {

			byte[] data =
				IOUtils.toByteArray (
					fileItem.getInputStream ());

			return mediaLogic.createMediaFromImageRequired (
				data,
				"image/jpeg",
				fileItem.getName ());

		} catch (IOException exception) {

			throw new RuntimeIoException (
				exception);

		}

	}

	@Override
	public
	Either<Optional<MediaRec>,String> formToInterface (
			@NonNull FormFieldSubmission submission) {

		return successResult (
			Optional.fromNullable (
				formValue (
					submission)));

	}

	@Override
	public
	String interfaceToHtmlSimple (
			@NonNull Container container,
			@NonNull Optional<MediaRec> interfaceValue,
			boolean link) {

		if (! interfaceValue.isPresent ()) {
			return "";
		}

		return stringFormat (
			"%s\n",
			mediaConsoleLogic.mediaThumb32 (
				interfaceValue.get ()),
			"%h",
			interfaceValue.get ().getFilename ());

	}

	@Override
	public
	String interfaceToHtmlComplex (
			@NonNull Container container,
			@NonNull Optional<MediaRec> interfaceValue) {

		if (! interfaceValue.isPresent ()) {
			return "";
		}

		return stringFormat (
			"%s<br>\n",
			mediaConsoleLogic.mediaThumb100 (
				interfaceValue.get ()),
			"%h (%h bytes)",
			interfaceValue.get ().getFilename (),
			interfaceValue.get ().getContent ().getData ().length);

	}

}
