package wbs.console.helper.provider;

import static wbs.utils.etc.Misc.isNotNull;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.OptionalUtils.optionalCast;
import static wbs.utils.etc.OptionalUtils.optionalOrNull;
import static wbs.utils.string.StringUtils.naivePluralise;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;
import static wbs.utils.string.StringUtils.stringSplitColon;
import static wbs.utils.string.StringUtils.underscoreToCamel;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.context.ConsoleContextStuff;
import wbs.console.context.ConsoleContextStuffSpec;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.helper.spec.ConsoleHelperProviderSpec;
import wbs.console.helper.spec.PrivKeySpec;
import wbs.console.helper.spec.RunPostProcessorSpec;
import wbs.console.module.ConsoleManager;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;
import wbs.console.request.Cryptor;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.annotations.WeakSingletonDependency;
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectManager;
import wbs.utils.etc.PropertyUtils;
import wbs.utils.string.StringSubstituter;

@Accessors (fluent = true)
@PrototypeComponent ("genericConsoleHelperProvider")
public
class GenericConsoleHelperProvider <
	RecordType extends Record <RecordType>
>
	implements ConsoleHelperProvider <RecordType> {

	// singleton dependencies

	@SingletonDependency
	ComponentManager componentManager;

	@WeakSingletonDependency
	ConsoleManager consoleManager;

	@WeakSingletonDependency
	ConsoleObjectManager consoleObjectManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	@WeakSingletonDependency
	ConsoleRequestContext requestContext;

	@WeakSingletonDependency
	UserPrivChecker privChecker;

	// required properties

	@Getter @Setter
	ConsoleHelperProviderSpec consoleHelperProviderSpec;

	@Getter @Setter
	ObjectHelper <RecordType> objectHelper;

	@Getter @Setter
	Class <ConsoleHelper <RecordType>> consoleHelperClass;

	// console helper properties

	@Getter @Setter
	Class <RecordType> objectClass;

	@Getter @Setter
	String objectName;

	@Getter @Setter
	String idKey;

	@Getter @Setter
	Cryptor cryptor;

	@Getter @Setter
	String defaultListContextName;

	@Getter @Setter
	String defaultObjectContextName;

	@Getter @Setter
	String viewDelegateField;

	@Getter @Setter
	String viewDelegatePrivCode;

	// state

	List<PrivKeySpec> viewPrivKeySpecs;

	// init

	public
	GenericConsoleHelperProvider <RecordType> init (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"init");

		// check required properties

		if (consoleHelperProviderSpec == null)
			throw new NullPointerException ("consoleHelperProviderSpec");

		if (objectHelper == null)
			throw new NullPointerException ("objectHelper");

		if (consoleHelperClass == null)
			throw new NullPointerException ("consoleHelperClass");

		// initialise other stuff

		objectClass (
			objectHelper.objectClass ());

		objectName (
			objectHelper.objectName ());

		idKey (
			consoleHelperProviderSpec.idKey ());

		defaultListContextName (
			ifNull (
				consoleHelperProviderSpec.defaultListContextName (),
				naivePluralise (
					objectName ())));

		defaultObjectContextName (
			ifNull (
				consoleHelperProviderSpec.defaultObjectContextName (),
				objectHelper.objectName ()));

		if (
			isNotNull (
				consoleHelperProviderSpec.viewPriv ())
		) {

			List <String> viewPrivParts =
				stringSplitColon (
					consoleHelperProviderSpec.viewPriv ());

			if (viewPrivParts.size () == 1) {

				viewDelegateField (
					viewPrivParts.get (0));

			} else if (viewPrivParts.size () == 2) {

				viewDelegateField (
					viewPrivParts.get (0));

				viewDelegatePrivCode (
					viewPrivParts.get (1));

			} else {

				throw new RuntimeException ();

			}

		}

		if (consoleHelperProviderSpec.cryptorBeanName () != null) {

			cryptor (
				componentManager.getComponentRequired (
					taskLogger,
					consoleHelperProviderSpec.cryptorBeanName (),
					Cryptor.class));

		}

		// collect view priv key specs

		String viewPrivKey =
			stringFormat (
				"%s.view",
				objectName ());

		for (
			PrivKeySpec privKeySpec
				: consoleHelperProviderSpec.privKeys ()
		) {

			if (
				stringNotEqualSafe (
					privKeySpec.name (),
					viewPrivKey)
			) {
				continue;
			}

			if (viewPrivKeySpecs == null) {

				viewPrivKeySpecs =
					new ArrayList<> ();

			}

			viewPrivKeySpecs.add (
				privKeySpec);

		}

		// and return

		return this;

	}

	@Override
	public
	void postProcess (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ConsoleContextStuff contextStuff) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"postProcess");

		taskLogger.debugFormat (
			"Running post processor for %s",
			objectName ());

		// lookup object

		Long id =
			(Long)
			contextStuff.get (
				idKey ());

		Record<?> object =
			objectHelper.findRequired (
				id);

		// set context stuff

		for (
			ConsoleContextStuffSpec contextStuffSpec
				: consoleHelperProviderSpec.contextStuffs ()
		) {

			if (
				contextStuffSpec.fieldName () != null
				&& contextStuffSpec.template () != null
			) {
				throw new RuntimeException ();
			}

			if (
				contextStuffSpec.delegateName () != null
				&& contextStuffSpec.fieldName () == null
			) {
				throw new RuntimeException ();
			}

			if (contextStuffSpec.template () != null) {

				contextStuff.set (
					contextStuffSpec.name (),
					contextStuff.substitutePlaceholders (
						contextStuffSpec.template ()));

			} else {

				Object target =
					contextStuffSpec.delegateName () != null
						? objectManager.dereference (
							object,
							contextStuffSpec.delegateName ())
						: object;

				contextStuff.set (
					contextStuffSpec.name (),
					PropertyUtils.getProperty (
						target,
						contextStuffSpec.fieldName ()));

			}

		}

		// set privs

		UserPrivChecker privChecker =
			(UserPrivChecker)
			this.privChecker;

		for (
			PrivKeySpec privKeySpec
				: consoleHelperProviderSpec.privKeys ()
		) {

			Record <?> privObject =
				privKeySpec.delegateName () != null
					? (Record <?>) objectManager.dereference (
						object,
						privKeySpec.delegateName ())
					: object;

			if (
				privChecker.canRecursive (
					privObject,
					privKeySpec.privName ())
			) {

				contextStuff.grant (
					privKeySpec.name ());

			}

		}

		// run chained post processors

		for (
			RunPostProcessorSpec runPostProcessorSpec
				: consoleHelperProviderSpec.runPostProcessors ()
		) {

			consoleManager.runPostProcessors (
				taskLogger,
				runPostProcessorSpec.name (),
				contextStuff);

		}

	}

	@Override
	public
	String getPathId (
			@NonNull Long objectId) {

		if (cryptor != null) {

			return cryptor.encryptInteger (
				objectId);

		} else {

			return Long.toString (
				objectId);

		}

	}

	@Override
	public
	String getDefaultContextPath (
			@NonNull RecordType object) {

		StringSubstituter stringSubstituter =
			new StringSubstituter ();

		if (objectHelper.typeCodeExists ()) {

			stringSubstituter

				.param (
					"typeCode",
					objectHelper.getTypeCode (
						object))

				.param (
					"typeCamel",
					underscoreToCamel (
						objectHelper.getTypeCode (
							object)));

		}

		String url =
			stringFormat (

				"/%s",
				defaultObjectContextName (),

				"/%s",
				getPathId (
					object.getId ()));

		return stringSubstituter.substitute (
			url);

	}

	@Override
	public
	String localPath (
			@NonNull RecordType object) {

		String urlTemplate =

			stringFormat (
				"/-/%s",
				defaultObjectContextName (),

				"/%s",
				getPathId (
					object.getId ()));

		StringSubstituter stringSubstituter =
			new StringSubstituter ();

		if (objectHelper.typeCodeExists ()) {

			stringSubstituter.param (
				"typeCode",
				objectHelper.getTypeCode (
					object));

		}

		return stringSubstituter.substitute (
			urlTemplate);

	}

	@Override
	public
	boolean canView (
			@NonNull RecordType object) {

		// types are always visible

		if (objectHelper.type ()) {
			return true;
		}

		// objects with cryptors are always visible

		if (
			isNotNull (
				cryptor)
		) {
			return true;
		}

		// view privs

		if (viewPrivKeySpecs != null) {

			for (
				PrivKeySpec privKeySpec
					: viewPrivKeySpecs
			) {

				Record <?> privObject =
					privKeySpec.delegateName () != null
						? (Record <?>) objectManager.dereference (
							object,
							privKeySpec.delegateName ())
						: object;

				if (
					privChecker.canRecursive (
						privObject,
						privKeySpec.privName ())
				) {
					return true;
				}

			}

		}

		// view delegate

		if (
			isNotNull (
				viewDelegateField)
		) {

			if (
				stringEqualSafe (
					viewDelegateField,
					"public")
			) {
				return true;
			}

			Record <?> delegate =
				(Record <?>)
				objectManager.dereference (
					object,
					viewDelegateField);

			if (viewDelegatePrivCode != null) {

				return privChecker.canRecursive (
					delegate,
					viewDelegatePrivCode);

			} else {

				ConsoleHelper <?> delegateHelper =
					consoleObjectManager.findConsoleHelperRequired (
						delegate);

				return delegateHelper.canViewGeneric (
					delegate);

			}

		}

		// default

		return privChecker.canRecursive (
			object);

	}

	@Override
	public
	RecordType lookupObject (
			@NonNull ConsoleContextStuff contextStuff) {

		Long objectId =
			(Long)
			contextStuff.get (
				idKey);

		return objectClass ().cast (
			optionalOrNull (
				optionalCast (
					Record.class,
					objectHelper.find (
						objectId))));

	}

}