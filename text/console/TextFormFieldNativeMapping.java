package wbs.platform.text.console;

import javax.inject.Inject;

import lombok.NonNull;
import lombok.experimental.Accessors;

import com.google.common.base.Optional;

import wbs.console.forms.FormFieldNativeMapping;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.platform.text.model.TextObjectHelper;
import wbs.platform.text.model.TextRec;

@Accessors (fluent = true)
@PrototypeComponent ("textFormFieldNativeMapping")
public
class TextFormFieldNativeMapping<Container>
	implements FormFieldNativeMapping<Container,String,TextRec> {

	// dependencies

	@Inject
	TextObjectHelper textHelper;

	// implementation

	@Override
	public
	Optional<TextRec> genericToNative (
			@NonNull Container container,
			@NonNull Optional<String> genericValue) {

		if (! genericValue.isPresent ()) {
			return Optional.<TextRec>absent ();
		}

		return Optional.of (
			textHelper.findOrCreate (
				genericValue.get ()));

	}

	@Override
	public
	Optional<String> nativeToGeneric (
			@NonNull Container container,
			@NonNull Optional<TextRec> nativeValue) {

		if (! nativeValue.isPresent ()) {
			return Optional.<String>absent ();
		}

		return Optional.of (
			nativeValue.get ().getText ());

	}

}
