package wbs.platform.console.helper;

import static wbs.framework.utils.etc.Misc.equal;
import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.stringFormat;
import static wbs.framework.utils.etc.Misc.underscoreToCamel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.application.context.ApplicationContext;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectManager;
import wbs.framework.record.Record;
import wbs.framework.utils.StringSubstituter;
import wbs.framework.utils.etc.BeanLogic;
import wbs.platform.console.context.ConsoleContextStuff;
import wbs.platform.console.context.ConsoleContextStuffSpec;
import wbs.platform.console.module.ConsoleManager;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.console.request.Cryptor;
import wbs.platform.priv.console.PrivChecker;
import wbs.platform.scaffold.console.RootConsoleHelper;

@Accessors (fluent = true)
@Log4j
@PrototypeComponent ("genericConsoleHelperProvider")
@SuppressWarnings ({ "rawtypes", "unchecked" })
public
class GenericConsoleHelperProvider
	implements ConsoleHelperProvider {

	// dependencies

	@Inject
	ApplicationContext applicationContext;

	@Inject
	ObjectManager objectManager;

	// indirect dependencies

	@Inject
	Provider<ConsoleHelperProviderRegistry> consoleHelperProviderRegistry;

	@Inject
	Provider<ConsoleManager> consoleManager;

	@Inject
	Provider<ConsoleObjectManager> consoleObjectManager;

	@Inject
	Provider<ConsoleRequestContext> requestContext;

	@Inject
	Provider<PrivChecker> privChecker;

	@Inject
	Provider<RootConsoleHelper> rootHelper;

	// required properties

	/*
	@Getter @Setter
	ConsoleModuleMiniSpec consoleModuleMiniSpec;
	*/

	@Getter @Setter
	ConsoleHelperProviderSpec consoleHelperProviderSpec;

	@Getter @Setter
	ObjectHelper objectHelper;

	@Getter @Setter
	Class consoleHelperClass;

	// console helper properties

	@Getter @Setter
	Class<Record<?>> objectClass;

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
	GenericConsoleHelperProvider init () {

		// check required properties

		/*
		if (consoleModuleMiniSpec == null)
			throw new NullPointerException ("consoleModuleMiniSpec");
		*/

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
				objectHelper.objectName () + "s"));

		defaultObjectContextName (
			ifNull (
				consoleHelperProviderSpec.defaultObjectContextName (),
				objectHelper.objectName ()));

		viewDelegateField (
			consoleHelperProviderSpec.viewDelegateField ());

		viewDelegatePrivCode (
			consoleHelperProviderSpec.viewDelegatePrivCode ());

		if (consoleHelperProviderSpec.cryptorBeanName () != null) {

			cryptor (
				applicationContext.getBean (
					consoleHelperProviderSpec.cryptorBeanName (),
					Cryptor.class));

		}

		// collect view priv key specs

		String viewPrivKey =
			stringFormat (
				"%s_view",
				objectName ());

		for (PrivKeySpec privKeySpec
				: consoleHelperProviderSpec.privKeys ()) {

			if (! equal (
					privKeySpec.name (),
					viewPrivKey))
				continue;

			if (viewPrivKeySpecs == null) {

				viewPrivKeySpecs =
					new ArrayList<PrivKeySpec> ();

			}

			viewPrivKeySpecs.add (
				privKeySpec);

		}

		// register

		consoleHelperProviderRegistry.get ().register (
			this);

		// and return

		return this;

	}

	@Override
	public
	void postProcess (
			ConsoleContextStuff contextStuff) {

		log.debug (
			stringFormat (
				"Running post processor for %s",
				objectName ()));

		// lookup object

		Integer id =
			(Integer)
			contextStuff.get (idKey ());

		Record<?> object =
			objectHelper.find (id);

		// set context stuff

		for (ConsoleContextStuffSpec contextStuffSpec
				: consoleHelperProviderSpec.contextStuffs ()) {

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
					BeanLogic.getProperty (
						target,
						contextStuffSpec.fieldName ()));

			}

		}

		// set privs

		PrivChecker privChecker =
			(PrivChecker)
			this.privChecker.get ();

		for (PrivKeySpec privKeySpec
				: consoleHelperProviderSpec.privKeys ()) {

			Record<?> privObject =
				privKeySpec.delegateName () != null
					? objectManager.dereference (
						object,
						privKeySpec.delegateName ())
					: object;

			if (privChecker.can (
					privObject,
					privKeySpec.privName ())) {

				contextStuff.grant (
					privKeySpec.name ());

			}

		}

		// run chained post processors

		for (RunPostProcessorSpec runPostProcessorSpec
				: consoleHelperProviderSpec.runPostProcessors ()) {

			consoleManager.get ().runPostProcessors (
				runPostProcessorSpec.name (),
				contextStuff);

		}

	}

	@Override
	public
	String getPathId (
			Integer objectId) {

		if (cryptor != null) {

			return cryptor.encryptInt (
				objectId);

		} else {

			return Integer.toString (
				objectId);

		}

	}

	@Override
	public
	String getDefaultContextPath (
			Record object) {

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
			Record object) {

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
			Record object) {

		// view privs

		if (viewPrivKeySpecs != null) {

			for (PrivKeySpec privKeySpec
					: viewPrivKeySpecs) {

				Record privObject =
					privKeySpec.delegateName () != null
						? objectManager.dereference (
							object,
							privKeySpec.delegateName ())
						: object;

				if (privChecker.get ().can (
						privObject,
						privKeySpec.privName ()))
					return true;

			}

		}

		// view delegate

		if (viewDelegateField != null) {

			Record<?> delegate =
				objectManager.dereference (
					object,
					viewDelegateField);

			if (viewDelegatePrivCode != null) {

				return privChecker.get ().can (
					delegate,
					viewDelegatePrivCode);

			} else {

				ConsoleHelper<?> delegateHelper =
					consoleObjectManager.get ().getConsoleObjectHelper (
						delegate);

				return delegateHelper.canView (
					delegate);

			}

		}

		// default

		return privChecker.get ().can (object);

	}

	@Override
	public
	Record<?> lookupObject (
			ConsoleContextStuff contextStuff) {

		int objectId =
			(Integer)
			contextStuff.get (idKey);

		Record<?> object =
			objectHelper.find (objectId);

		return object;

	}

}
