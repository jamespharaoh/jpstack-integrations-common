package wbs.platform.console.forms;

import static wbs.framework.utils.etc.Misc.equal;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.record.PermanentRecord;
import wbs.platform.console.html.ScriptRef;

@Accessors (fluent = true)
@PrototypeComponent ("updatableFormField")
@DataClass ("updatable-form-field")
public
class UpdatableFormField<Container,Generic,Native,Interface>
	implements FormField<Container,Generic,Native,Interface> {

	// properties

	@Getter
	Boolean virtual = false;

	@DataAttribute
	@Getter @Setter
	Boolean large = false;

	@DataAttribute
	@Getter @Setter
	String name;

	@DataAttribute
	@Getter @Setter
	String label;

	@Getter @Setter
	Set<ScriptRef> scriptRefs =
		new LinkedHashSet<ScriptRef> ();

	@Getter @Setter
	FormFieldAccessor<Container,Native> accessor;

	@Getter @Setter
	FormFieldNativeMapping<Generic,Native> nativeMapping;

	@Getter @Setter
	FormFieldValueValidator<Generic> valueValidator;

	@Getter @Setter
	FormFieldConstraintValidator<Container,Native> constraintValidator;

	@Getter @Setter
	FormFieldInterfaceMapping<Container,Generic,Interface> interfaceMapping;

	@Getter @Setter
	FormFieldRenderer<Container,Interface> renderer;

	@Getter @Setter
	FormFieldUpdateHook<Container,Generic,Native> updateHook;

	// implementation

	public
	void init (
			String fieldSetName) {

		if (valueValidator == null) {

			throw new NullPointerException (
				stringFormat (
					"No value validator for %s.%s",
					fieldSetName,
					name));

		}

		if (interfaceMapping == null) {

			throw new NullPointerException (
				stringFormat (
					"No interface mapping for %s.%s",
					fieldSetName,
					name));

		}

	}

	@Override
	public
	void renderTableCell (
			PrintWriter out,
			Container container,
			boolean link) {

		Native nativeValue =
			accessor.read (
				container);

		Generic genericValue =
			nativeMapping.nativeToGeneric (
				nativeValue);

		Interface interfaceValue =
			interfaceMapping.genericToInterface (
				container,
				genericValue);

		renderer.renderTableCell (
			out,
			container,
			interfaceValue,
			link);

	}

	@Override
	public
	void renderFormRow (
			PrintWriter out,
			Container container) {

		Native nativeValue =
			accessor.read (
				container);

		Generic genericValue =
			nativeMapping.nativeToGeneric (
				nativeValue);

		Interface interfaceValue =
			interfaceMapping.genericToInterface (
				container,
				genericValue);

		renderer.renderFormRow (
			out,
			container,
			interfaceValue);

	}

	@Override
	public
	void update (
			Container container,
			UpdateResult<Generic,Native> updateResult) {

		List<String> errors =
			new ArrayList<String> ();

		// do nothing if no value present in form

		if (! renderer.formValuePresent ()) {

			updateResult
				.updated (false);

			return;

		}

		// get interface value from form

		Interface newInterfaceValue =
			renderer.formToInterface (
				errors);

		if (! errors.isEmpty ()) {

			updateResult
				.updated (false)
				.errors (errors);

			return;

		}

		// convert to generic

		Generic newGenericValue =
			interfaceMapping.interfaceToGeneric (
				container,
				newInterfaceValue,
				errors);

		if (! errors.isEmpty ()) {

			updateResult
				.updated (false)
				.errors (errors);

			return;

		}

		// perform value validation

		valueValidator.validate (
			newGenericValue,
			errors);

		if (! errors.isEmpty ()) {

			updateResult
				.updated (false)
				.errors (errors);

		}

		// convert to native

		Native newNativeValue =
			nativeMapping.genericToNative (
				newGenericValue);

		// check new value

		constraintValidator.validate (
			container,
			newNativeValue,
			errors);

		if (! errors.isEmpty ()) {

			updateResult
				.updated (false)
				.errors (errors);

			return;

		}

		// get the current value, if it is the same, do nothing

		Native oldNativeValue =
			accessor.read (
				container);

		Generic oldGenericValue =
			nativeMapping.nativeToGeneric (
				oldNativeValue);

		if (equal (
				oldGenericValue,
				newGenericValue)) {

			updateResult
				.updated (false);

			return;

		}

		// set the new value

		accessor.write (
			container,
			newNativeValue);

		updateResult

			.updated (true)

			.oldGenericValue (oldGenericValue)
			.newGenericValue (newGenericValue)

			.oldNativeValue (oldNativeValue)
			.newNativeValue (newNativeValue);

	}

	@Override
	public
	void runUpdateHook (
			UpdateResult<Generic,Native> updateResult,
			Container container,
			PermanentRecord<?> linkObject,
			Object objectRef,
			String objectType) {

		updateHook.onUpdate (
			updateResult,
			container,
			linkObject,
			objectRef,
			objectType);

	}

}
