package wbs.console.forms;

import static wbs.framework.utils.etc.Misc.isNotNull;
import static wbs.framework.utils.etc.Misc.isNotPresent;

import java.util.List;

import javax.inject.Inject;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.google.common.base.Optional;

import wbs.console.helper.ConsoleObjectManager;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.record.Record;

@Accessors (fluent = true)
@PrototypeComponent ("objectCsvFormFieldInterfaceMapping")
public
class ObjectCsvFormFieldInterfaceMapping<Container,Generic extends Record<Generic>>
	implements FormFieldInterfaceMapping<Container,Generic,String> {

	// dependencies

	@Inject
	ConsoleObjectManager objectManager;

	// properties

	@Getter @Setter
	String rootFieldName;

	// implementation

	@Override
	public
	Optional<Generic> interfaceToGeneric (
			@NonNull Container container,
			@NonNull Optional<String> interfaceValue,
			@NonNull List<String> errors) {

		throw new UnsupportedOperationException ();

	}

	@Override
	public
	Optional<String> genericToInterface (
			@NonNull Container container,
			@NonNull Optional<Generic> genericValue) {

		if (
			isNotPresent (
				genericValue)
		) {

			return Optional.<String>of ("");

		} else {

			Optional<Record<?>> root;

			if (
				isNotNull (
					rootFieldName)
			) {

				root =
					Optional.<Record<?>>of (
						(Record<?>)
						objectManager.dereference (
							container,
							rootFieldName));

			} else {

				root =
					Optional.<Record<?>>absent ();

			}

			return Optional.of (
				objectManager.objectPathMini (
					genericValue.get (),
					root));

		}

	}

}
