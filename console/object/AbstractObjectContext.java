package wbs.platform.console.object;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.extern.log4j.Log4j;
import wbs.framework.record.Record;
import wbs.framework.web.PageNotFoundException;
import wbs.platform.console.context.ConsoleContext;
import wbs.platform.console.context.ConsoleContextStuff;
import wbs.platform.console.helper.ConsoleObjectManager;
import wbs.platform.console.lookup.ObjectLookup;
import wbs.platform.console.lookup.StringLookup;
import wbs.platform.console.module.ConsoleManager;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.console.request.Cryptor;

@Log4j
public abstract
class AbstractObjectContext
	extends ConsoleContext {

	// dependencies

	@Inject
	ConsoleObjectManager objectManager;

	// indirect dependencies

	@Inject
	Provider<ConsoleManager> consoleManager;

	// abstract getters

	public abstract
	Cryptor cryptor ();

	public abstract
	String requestIdKey ();

	public abstract
	String title ();

	public abstract
	StringLookup titleLookup ();

	public abstract
	ObjectLookup<?> objectLookup ();

	public abstract
	String postProcessorName ();

	public abstract
	Map<String,Object> stuff ();

	// implementation

	@Override
	public
	String localPathForStuff (
			ConsoleContextStuff stuff) {

		return stringFormat (
			"/%s",
			encodeId (
				(Integer)
				stuff.get (
					requestIdKey ())));

	}

	@Override
	public
	String titleForStuff (
			ConsoleContextStuff stuff) {

		if (title () != null) {

			return stuff.substitutePlaceholders (
				title ());

		}

		if (titleLookup () != null) {

			return titleLookup ().lookup (
				stuff);

		}

		throw new RuntimeException ();

	}

	protected
	int decodeId (
			String encodedId) {

		if (cryptor () != null) {

			return cryptor ().decryptInt (
				encodedId);

		} else {

			return Integer.parseInt (
				encodedId);

		}

	}

	protected
	String encodeId (
			int numericId) {

		if (cryptor () != null) {

			return cryptor ().encryptInt (
				numericId);

		} else {

			return Integer.toString (
				numericId);

		}

	}

	@Override
	public
	void initContext (
			PathSupply pathParts,
			ConsoleContextStuff contextStuff) {

		int localId =
			decodeId (pathParts.next ());

		contextStuff.set (
			requestIdKey (),
			localId);

		Object object =
			objectLookup ().lookupObject (
				contextStuff);

		if (object == null) {

			log.warn ("Can't find object with id " + localId);

			throw new PageNotFoundException ();

		}

		if (stuff () != null) {

			for (Map.Entry<String,? extends Object> entry
					: stuff ().entrySet ()) {

				contextStuff.set (
					entry.getKey (),
					entry.getValue ());

			}

		}

		if (postProcessorName () != null) {

			consoleManager.get ()
				.runPostProcessors (
					postProcessorName (),
					contextStuff);

		}

	}

	public static
	interface ObjectPostProcessor {

		void process (
				ConsoleRequestContext requestContext,
				ConsoleContextStuff contextStuff,
				Record<?> object);

	}

}
