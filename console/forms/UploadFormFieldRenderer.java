package wbs.platform.console.forms;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.platform.console.request.ConsoleRequestContext;

@PrototypeComponent ("uploadFormFieldRenderer")
@Accessors (fluent = true)
public
class UploadFormFieldRenderer<Container>
	implements FormFieldRenderer<Container,FileUpload> {

	// dependencies

	@Inject
	ConsoleRequestContext requestContext;

	// properties

	@Getter @Setter
	String name;

	@Getter @Setter
	String label;

	@Getter @Setter
	Integer size;

	// details

	@Getter
	boolean fileUpload = true;

	// implementation

	@Override
	public
	void renderTableCell (
			PrintWriter out,
			Container container,
			FileUpload interfaceValue,
			boolean link) {

		out.write (
			stringFormat (
				"<td>%s</td>\n",
				interfaceToHtml (
					container,
					interfaceValue,
					link)));

	}

	@Override
	public
	void renderTableRow (
			PrintWriter out,
			Container container,
			FileUpload interfaceValue,
			boolean link) {

		out.write (
			stringFormat (
				"<tr>\n",
				"<th>%h</th>\n",
				label ()));

		renderTableCell (
			out,
			container,
			interfaceValue,
			link);

		out.write (
			stringFormat (
				"</tr>\n"));

	}

	@Override
	public
	void renderFormRow (
			PrintWriter out,
			Container container,
			FileUpload interfaceValue) {

		out.write (
			stringFormat (
				"<tr>\n",
				"<th>%h</th>\n",
				label (),
				"<td>"));

		renderFormInput (
			out,
			container,
			interfaceValue);

		out.write (
			stringFormat (
				"</td>\n",
				"</tr>\n"));

	}

	@Override
	public
	void renderFormInput (
			PrintWriter out,
			Container container,
			FileUpload interfaceValue) {

		out.write (
			stringFormat (
				"<input",
				" type=\"file\"",
				" size=\"%h\"",
				size (),
				" name=\"%h\"",
				name (),
				" value=\"%h\"",
				formValuePresent ()
					? formValue ()
					: interfaceValue,
				">\n"));

	}

	@Override
	public
	boolean formValuePresent () {

		if (! requestContext.isMultipart ())
			return false;

		FileItem fileItem =
			requestContext.fileItemFile (
				name ());

		return fileItem != null;

	}

	@SneakyThrows (IOException.class)
	FileUpload formValue () {

		FileItem fileItem =
			requestContext.fileItemFile (
				name ());

		if (fileItem == null)
			return null;

		byte[] data =
			IOUtils.toByteArray (
				fileItem.getInputStream ());

		return new FileUpload ()

			.name (
				fileItem.getName ())

			.data (
				data);

	}

	@Override
	public
	FileUpload formToInterface (
			List<String> errors) {

		return formValue ();

	}

	@Override
	public
	String interfaceToHtml (
			Container container,
			FileUpload interfaceValue,
			boolean link) {

		if (interfaceValue == null)
			return "";

		return stringFormat (
			"%h (%h bytes)",
			interfaceValue.name (),
			interfaceValue.data ().length);

	}

}
