package wbs.platform.console.module;

import static wbs.framework.utils.etc.Misc.camelToHyphen;
import static wbs.framework.utils.etc.Misc.equal;
import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.isNotNull;
import static wbs.framework.utils.etc.Misc.joinWithSeparator;
import static wbs.framework.utils.etc.Misc.joinWithoutSeparator;
import static wbs.framework.utils.etc.Misc.pluralise;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.data.tools.DataToXml;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.web.ExternalRedirectException;
import wbs.framework.web.PageNotFoundException;
import wbs.framework.web.PathHandler;
import wbs.framework.web.Responder;
import wbs.framework.web.ServletModule;
import wbs.framework.web.WebFile;
import wbs.platform.console.context.ConsoleContext;
import wbs.platform.console.context.ConsoleContext.PathSupply;
import wbs.platform.console.context.ConsoleContextStuff;
import wbs.platform.console.context.ConsoleContextType;
import wbs.platform.console.helper.ConsoleHelperProvider;
import wbs.platform.console.helper.ConsoleHelperProviderRegistry;
import wbs.platform.console.helper.ConsoleObjectManager;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.console.tab.ConsoleContextTab;
import wbs.platform.console.tab.ContextTabPlacement;
import wbs.platform.supervisor.SupervisorConfig;

@Log4j
@SingletonComponent ("consoleManager")
public
class ConsoleManagerImplementation
	implements
		ConsoleManager,
		ServletModule {

	// dependencies

	@Inject
	ConsoleHelperProviderRegistry consoleHelperProviderRegistry;

	@Inject
	ConsoleObjectManager consoleObjectManager;

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	Database database;

	@Inject
	Provider<ConsoleContextStuff> contextStuffProvider;

	@Inject @Getter
	Map<String,ConsoleModule> consoleModules;

	// state

	Map<String,ConsoleContextType> contextTypesByName =
		new HashMap<String,ConsoleContextType> ();

	Map<String,ConsoleContext> consoleContextsByName =
		new HashMap<String,ConsoleContext> ();

	List<ConsoleContext> consoleContexts =
		new ArrayList<ConsoleContext> ();

	Map<String,ConsoleContextTab> contextTabs =
		new HashMap<String,ConsoleContextTab> ();

	Map<String,WebFile> contextFiles =
		new HashMap<String,WebFile> ();

	Map<String,Provider<Responder>> responders =
		new HashMap<String,Provider<Responder>> ();

	Map<Pair<String,String>,List<ConsoleContext>> contextsByParentAndType =
		new HashMap<Pair<String,String>,List<ConsoleContext>> ();

	Map<String,List<ConsoleContext>> contextsWithoutParentByType =
		new HashMap<String,List<ConsoleContext>> ();

	Map<String,SupervisorConfig> supervisorConfigs =
		new HashMap<String,SupervisorConfig> ();

	// implementation

	@Override
	public
	ConsoleContext context (
			@NonNull String contextName,
			boolean required) {

		ConsoleContext context =
			consoleContextsByName.get (
				contextName);

		if (context == null && required) {

			throw new RuntimeException (
				stringFormat (
					"No such context: %s",
					contextName));

		}

		return context;

	}

	@Override
	public
	ConsoleContextTab tab (
			@NonNull String tabName,
			boolean required) {

		ConsoleContextTab tab =
			contextTabs.get (
				tabName);

		if (tab == null && required) {

			throw new RuntimeException (
				stringFormat (
					"No such tab: %s",
					tabName));

		}

		return tab;

	}

	public
	WebFile contextFile (
			@NonNull String name) {

		return contextFiles.get (
			name);

	}

	/**
	 * Queries the application context for console modules and sets up their
	 * contexts, tabs and files automatically.
	 */
	@PostConstruct
	public
	void init () {

		int errors = 0;

		dumpData ();

		// find responders

		errors +=
			findResponders ();

		errors +=
			collectContextTypes ();

		errors +=
			collectContextTabs ();

		errors +=
			addTabsToContextTypes ();

		errors +=
			collectContextFiles ();

		errors +=
			collectContextTypeFiles ();

		errors +=
			resolveContextTabs ();

		errors +=
			collectConsoleContexts ();

		errors +=
			collectContextsByParentType ();

		errors +=
			collectSupervisorConfigs ();

		// abandon if we encountered errors

		if (errors != 0) {

			throw new RuntimeException (
				stringFormat (
					"%s initialising context manager",
					pluralise (errors, "error")));

		}

	}

	int collectContextTypes () {

		int errors = 0;

		Map<String,String> beanNamesByContextTypeName =
			new HashMap<String,String> ();

		for (
			Map.Entry<String,ConsoleModule> ent
				: consoleModules.entrySet ()
		) {

			String beanName =
				ent.getKey ();

			ConsoleModule consoleModule =
				ent.getValue ();

			for (
				ConsoleContextType contextType
					: consoleModule.contextTypes ()
			) {

				if (contextTypesByName.containsKey (contextType.name ())) {

					log.error (
						stringFormat (
							"Duplicated context type %s in %s and %s",
							contextType.name (),
							beanName,
							beanNamesByContextTypeName.get (
								contextType.name ())));

					errors ++;

					continue;

				}

				beanNamesByContextTypeName.put (
					contextType.name (),
					beanName);

				contextTypesByName.put (
					contextType.name (),
					contextType);

				log.debug (
					stringFormat (
						"Adding context type %s from %s",
						contextType.name (),
						beanName));

			}

		}

		return errors;

	}

	int collectContextTabs () {

		int errors = 0;

		for (
			Map.Entry<String,ConsoleModule> consoleModuleEntry
				: consoleModules.entrySet ()
		) {

			String consoleModuleName =
				consoleModuleEntry.getKey ();

			ConsoleModule consoleModule =
				consoleModuleEntry.getValue ();

			for (
				ConsoleContextTab contextTab
					: consoleModule.tabs ()
			) {

				if (contextTabs.containsKey (
						contextTab.name ())) {

					log.error (
						stringFormat (
							"Duplicated tab %s in %s",
							contextTab.name (),
							consoleModuleName));

					errors ++;

					continue;

				}

				contextTabs.put (
					contextTab.name (),
					contextTab);

				log.debug (
					stringFormat (
						"Adding tab %s from %s",
						contextTab.name (),
						consoleModuleName));

			}

		}

		return errors;

	}

	int addTabsToContextTypes () {

		int errors = 0;

		for (
			Map.Entry<String,ConsoleModule> entry
				: consoleModules.entrySet ()
		) {

			String consoleModuleName =
				entry.getKey ();

			ConsoleModule consoleModule =
				entry.getValue ();

			for (
				Map.Entry<String,List<ContextTabPlacement>> tabPlacementsEntry
					: consoleModule
						.tabPlacementsByContextType ()
						.entrySet ()
			) {

				String contextTypeName =
					tabPlacementsEntry.getKey ();

				List<ContextTabPlacement> tabPlacements =
					tabPlacementsEntry.getValue ();

				ConsoleContextType contextType =
					contextTypesByName.get (
						contextTypeName);

				if (contextType == null) {

					List<String> contextTabNames =
						new ArrayList<String> ();

					for (
						ContextTabPlacement tabPlacement
							: tabPlacements
					) {

						contextTabNames.add (
							stringFormat (
								"%s:%s",
								tabPlacement.tabLocation (),
								tabPlacement.tabName ()));

					}

					log.error (
						stringFormat (
							"Unknown context type %s ",
							contextTypeName,
							"referenced from tabs %s ",
							joinWithSeparator (
								", ",
								contextTabNames),
							"in %s",
							consoleModuleName));

					errors ++;

					continue;

				}

				for (
					ContextTabPlacement tabPlacement
						: tabPlacements
				) {

					String contextTabName =
						tabPlacement.tabName ();

					if (contextTabName.charAt (0) != '+') {

						ConsoleContextTab contextTab =
							contextTabs.get (contextTabName);

						if (contextTab == null) {

							log.error (
								"Unknown tab " + contextTabName +
								" referenced from " + consoleModuleName);

							errors ++;

							continue;

						}

					}

					contextType.tabPlacements ().add (
						tabPlacement);

				}

			}

		}

		return errors;

	}

	int collectContextFiles () {

		int errors = 0;

		for (
			Map.Entry<String,ConsoleModule> consoleModuleEntry
				: consoleModules.entrySet ()
		) {

			String consoleModuleName =
				consoleModuleEntry.getKey ();

			ConsoleModule consoleModule =
				consoleModuleEntry.getValue ();

			for (
				Map.Entry<String,? extends WebFile> contextFileEntry
					: consoleModule.contextFiles ().entrySet ()
			) {

				String contextFileName =
					contextFileEntry.getKey ();

				WebFile contextFile =
					contextFileEntry.getValue ();

				if (contextFiles.containsKey (
						contextFileName)) {

					log.error (
						stringFormat (
							"Duplicated context file: %s",
							contextFileName));

					errors ++;

					continue;

				}

				contextFiles.put (
					contextFileName,
					contextFile);

				log.debug (
					stringFormat (
						"Adding file %s from %s",
						contextFileName,
						consoleModuleName));

			}

		}

		return errors;

	}

	int collectContextTypeFiles () {

		int errors = 0;

		for (
			Map.Entry<String,ConsoleModule> consoleModuleEntry
				: consoleModules.entrySet ()
		) {

			String consoleModuleName =
				consoleModuleEntry.getKey ();

			ConsoleModule consoleModule =
				consoleModuleEntry.getValue ();

			for (
				Map.Entry<String,? extends Collection<String>>
				contextFilesByContextTypeEntry
					: consoleModule
						.contextFilesByContextType ()
						.entrySet ()
			) {

				String contextTypeName =
					contextFilesByContextTypeEntry.getKey ();

				Collection<String> contextFileNames =
					contextFilesByContextTypeEntry.getValue ();

				ConsoleContextType contextType =
					contextTypesByName.get (contextTypeName);

				if (contextType == null) {

					log.error (
						stringFormat (
							"Unknown context type %s referenced from files in ",
							contextTypeName,
							"%s",
							consoleModuleName));

					errors ++;

					continue;

				}

				Map<String,WebFile> typeContextFiles =
					contextType.files ();

				for (
					String contextFileName
						: contextFileNames
				) {

					WebFile contextFile =
						contextFiles.get (contextFileName);

					if (contextFile == null) {

						log.error (
							stringFormat (
								"Unknown file %s references from context type ",
								contextFileName,
								"%s",
								contextTypeName));

						errors ++;

						continue;

					}

					typeContextFiles.put (
						contextFileName,
						contextFile);

				}

			}

		}

		return errors;

	}

	int resolveContextTabs () {

		int errors = 0;

		for (
			ConsoleContextType contextType
				: contextTypesByName.values ()
		) {

			try {

				contextType.resolveTabSpecs (
					contextTabs);

			} catch (Exception exception) {

				errors ++;

				log.info (
					stringFormat (
						"Dumping context type tab specification for %s",
						contextType.name ()));

				for (
					ContextTabPlacement tabPlacement
						: contextType.tabPlacements ()
				) {

					log.info (
						stringFormat (
							"%s => %s",
							tabPlacement.tabLocation (),
							tabPlacement.tabName ()));

				}

			}

		}

		return errors;

	}

	int collectConsoleContexts () {

		int errors = 0;

		for (
			Map.Entry<String,ConsoleModule> consoleModuleEntry
				: consoleModules.entrySet ()
		) {

			String consoleModuleName =
				consoleModuleEntry.getKey ();

			ConsoleModule consoleModule =
				consoleModuleEntry.getValue ();

			for (
				ConsoleContext consoleContext
					: consoleModule.contexts ()
			) {

				if (consoleContextsByName.containsKey (
						consoleContext.name ())) {

					log.error (
						stringFormat (
							"Duplicated context name: %s",
							consoleContext.name ()));

					errors ++;

					continue;

				}

				consoleContextsByName.put (
					consoleContext.name (),
					consoleContext);

				ConsoleContextType contextType =
					contextTypesByName.get (consoleContext.typeName ());

				if (contextType == null) {

					log.error (
						stringFormat (
							"Unknown context type %s referenced from context %s",
							consoleContext.typeName (),
							consoleContext.name (),
							" in %s",
							consoleModuleName));

					errors ++;

					continue;

				}

				consoleContext

					.contextType (
						contextType)

					.files (
						contextType.files ())

					.contextTabs (
						contextType.tabs ());

				log.debug (
					stringFormat (
						"Adding context %s from %s",
						consoleContext.name (),
						consoleModuleName));

			}

		}

		consoleContexts.addAll (
			consoleContextsByName.values ());

		Collections.sort (
			consoleContexts);

		return errors;

	}

	int collectContextsByParentType () {

		for (
			ConsoleContext context
				: consoleContexts
		) {

			if (context.parentContextName () != null) {

				Pair<String,String> key =
					Pair.<String,String>of (
						context.parentContextName (),
						context.typeName ());

				List<ConsoleContext> contextsForParentAndType =
					contextsByParentAndType.get (key);

				if (contextsForParentAndType == null) {

					contextsForParentAndType =
						new ArrayList<ConsoleContext> ();

					contextsByParentAndType.put (
						key,
						contextsForParentAndType);

				}

				contextsForParentAndType.add (
					context);

			} else {

				List<ConsoleContext> contextsForType =
					contextsWithoutParentByType.get (
						context.typeName ());

				if (contextsForType == null) {

					contextsForType =
						new ArrayList<ConsoleContext> ();

					contextsWithoutParentByType.put (
						context.typeName (),
						contextsForType);

				}

				contextsForType.add (
					context);

			}

		}

		return 0;

	}

	int collectSupervisorConfigs () {

		int errors = 0;

		for (
			Map.Entry<String,ConsoleModule> consoleModuleEntry
				: consoleModules.entrySet ()
		) {

			String consoleModuleName =
				consoleModuleEntry.getKey ();

			ConsoleModule consoleModule =
				consoleModuleEntry.getValue ();

			for (
				Map.Entry<String,SupervisorConfig> entry
					: consoleModule.supervisorConfigs ().entrySet ()
			) {

				String supervisorConfigName =
					entry.getKey ();

				SupervisorConfig supervisorConfig =
					entry.getValue ();

				if (
					supervisorConfigs.containsKey (
						supervisorConfigName)
				) {

					log.error (
						stringFormat (
							"Duplicated supervisor config name %s ",
							supervisorConfigName,
							" in console module %s",
							consoleModuleName));

					errors ++;

				}

				supervisorConfigs.put (
					supervisorConfigName,
					supervisorConfig);

			}

		}

		return errors;

	}

	void dumpData () {

		// delete old data

		try {

			FileUtils.deleteDirectory (
				new File (
					"work/console/module"));

			FileUtils.forceMkdir (
				new File (
					"work/console/module"));

		} catch (IOException exception) {

			log.error (
				"Error deleting contents of work/console/module",
				exception);

		}

		// iterate modules

		for (Map.Entry<String,ConsoleModule> consoleModuleEntry
				: consoleModules.entrySet ()) {

			String consoleModuleBeanName =
				consoleModuleEntry.getKey ();

			ConsoleModule consoleModule =
				consoleModuleEntry.getValue ();

			String outputFileName =
				stringFormat (
					"work/console/module/%s.xml",
					camelToHyphen (
						consoleModuleBeanName));

			try {

				new DataToXml ()
					.object (consoleModule)
					.write (outputFileName);

			} catch (IOException exception) {

				log.warn (
					stringFormat (
						"Error writing %s",
						outputFileName));

			}

		}

	}

	int findResponders () {

		int errors = 0;

		for (
			Map.Entry<String,ConsoleModule> consoleModuleEntry
				: consoleModules.entrySet ()
		) {

			String consoleModuleName =
				consoleModuleEntry.getKey ();

			ConsoleModule consoleModule =
				consoleModuleEntry.getValue ();

			log.debug (
				stringFormat (
					"Collecting responders from console module %s",
					consoleModuleName));

			for (
				Map.Entry<String,Provider<Responder>> responderEntry
					: consoleModule.responders ().entrySet ()
			) {

				String responderName =
					responderEntry.getKey ();

				if (
					responderName == null
					|| responderName.isEmpty ()
				) {

					log.error (
						stringFormat (
							"Empty repsponder name in %s",
							consoleModuleName));

					errors ++;

					continue;

				}

				if (responders.containsKey (responderName)) {

					log.error (
						stringFormat (
							"Duplicate responder name: %s",
							responderName));

					errors ++;

					continue;

				}

				Provider<Responder> responder =
					responderEntry.getValue ();

				if (responder == null)
					throw new RuntimeException ();

				log.debug (
					stringFormat (
						"Got reponder %s",
						responderName));

				responders.put (
					responderName,
					responder);

			}

		}

		return errors;

	}

	// ========================================================= servlet module

	@Override
	public
	Map<String,PathHandler> paths () {

		Map<String,ConsoleContext> contextsByPathPrefix =
			new HashMap<String,ConsoleContext> ();

		for (ConsoleContext context
				: consoleContextsByName.values ()) {

			if (! context.global ())
				continue;

			if (contextsByPathPrefix.containsKey (
					context.pathPrefix ())) {

				ConsoleContext existingContext =
					contextsByPathPrefix.get (
						context.pathPrefix ());

				log.error (
					stringFormat (
						"Duplicated context path %s for %s and %s",
						context.pathPrefix (),
						context.name (),
						existingContext.name ()));

			}

			contextsByPathPrefix.put (
				context.pathPrefix (),
				context);

		}

		Map<String,PathHandler> pathHandlersByPathPrefix =
			new HashMap<String,PathHandler> ();

		for (Map.Entry<String,ConsoleContext> contextEntry
				: contextsByPathPrefix.entrySet ()) {

			pathHandlersByPathPrefix.put (
				contextEntry.getKey (),
				new ContextPathHandler (
					contextEntry.getValue ()));

		}

		return pathHandlersByPathPrefix;

	}

	@Override
	public
	Map<String,WebFile> files () {
		return null;
	}

	private static
	PathSupply pathParts (
			String path) {

		List<String> ret =
			new ArrayList<String> ();

		int pos = 0;

		while (path.length () > pos) {

			if (path.length () < pos + 2) {

				throw new RuntimeException (
					stringFormat (
						"Invalid path: '%s'",
						path));

			}

			if (path.charAt (pos) != '/') {

				throw new RuntimeException (
					stringFormat (
						"Invalid path: '%s'",
						path));

			}

			int nextSlash =
				path.indexOf ('/', pos + 1);

			if (nextSlash == -1)
				nextSlash = path.length ();

			if (nextSlash == pos + 1) {

				throw new RuntimeException (
					stringFormat (
						"Invalid path: '%s'",
						path));

			}

			ret.add (
				path.substring (
					pos + 1,
					nextSlash));

			pos = nextSlash;

		}

		return new PathSupply (ret);

	}

	@Override
	public
	void changeContext (
			ConsoleContext context,
			String contextPartSuffix) {

		requestContext.changedContextPath (
			joinWithoutSeparator (
				context.pathPrefix (),
				contextPartSuffix));

		PathSupply pathParts =
			pathParts (contextPartSuffix);

		ConsoleContextStuff contextStuff =
			requestContext.contextStuff ();

		contextStuff.reset ();

		@Cleanup
		Transaction transaction =
			database.beginReadOnly (
				this);

		context.initContext (
			pathParts,
			contextStuff);

		context.initTabContext (
			contextStuff);

		transaction.close ();

		if (pathParts.size () > 0)
			throw new RuntimeException ();

		requestContext.request (
			"context",
			context);

	}

	@Override
	public
	void runPostProcessors (
			String name,
			ConsoleContextStuff contextStuff) {

		ConsoleHelperProvider<?> consoleHelperProvider =
			consoleHelperProviderRegistry.findByObjectName (
				name);

		if (consoleHelperProvider != null) {

			consoleHelperProvider.postProcess (
				contextStuff);

		}

	}

	/**
	 * Path handler for a context. Calls the context's initContext method then
	 * retrieves the appropriate file and return it.
	 */
	private
	class ContextPathHandler
		implements PathHandler {

		private final
		ConsoleContext consoleContext;

		private
		ContextPathHandler (
				@NonNull ConsoleContext newContext) {

			consoleContext =
				newContext;

		}

		@Override
		public
		WebFile processPath (
				String remainingPath) {

			String fullPath =
				joinWithoutSeparator (
					consoleContext.pathPrefix (),
					remainingPath);

			return fileForPath (
				fullPath);

		}

	}

	WebFile fileForPath (
			String path) {

		log.debug (
			stringFormat (
				"processing path %s",
				path));

		@Cleanup
		Transaction transaction =
			database.beginReadOnly (
				this);

		PathSupply pathParts =
			pathParts (path);

		ConsoleContextTab parentContextTab =
			null;

		String firstContextName =
			pathParts.next ();

		ConsoleContext consoleContext =
			context (
				firstContextName,
				true);

		ConsoleContextStuff contextStuff =
			null;

		requestContext.foreignContextPath (
			"");

		while (true) {

			log.debug (
				stringFormat (
					"starting lap with context %s",
					consoleContext.name ()));

			try {

				String contextStuffPath =
					stripLastPath (
						pathParts.used ());

				contextStuff =
					contextStuffProvider.get ()
						.foreignPath (contextStuffPath)
						.consoleContext (consoleContext)
						.parentContextStuff (contextStuff)
						.embeddedParentContextTab (parentContextTab);

				consoleContext.initContext (
					pathParts,
					contextStuff);

				consoleContext.initTabContext (
					contextStuff);

				log.debug (
					stringFormat (
						"context initialised"));

				if (pathParts.size () == 0) {

					log.debug (
						stringFormat (
							"no more path parts, looking up default file"));

					String defaultLocalFile =
						consoleContext
							.contextType ()
							.lookupDefaultFileName (requestContext);

					if (defaultLocalFile == null
							|| defaultLocalFile.length () == 0) {

						log.error (
							stringFormat (
								"context %s has no default file",
								consoleContext.name ()));

						throw new PageNotFoundException ();

					}

					log.debug (
						stringFormat (
							"got default file %s",
							defaultLocalFile));

					String defaultUrl =
						resolveLocalFile (
							contextStuff,
							contextStuff.consoleContext (),
							defaultLocalFile);

					log.debug (
						stringFormat (
							"redirecting to \"%s\"",
							defaultUrl));

					throw new ExternalRedirectException (
						requestContext.resolveApplicationUrl (
							contextStuff.substitutePlaceholders (
								defaultUrl)));

				}

				if (pathParts.size () == 1) {

					log.debug (
						stringFormat (
							"single path part left, looking up file"));

					String file =
						pathParts.next ();

					log.debug (
						stringFormat (
							"file name is %s",
							file));

					requestContext.request (
						"context",
						consoleContext);

					requestContext.request (
						"contextStuff",
						contextStuff);

					WebFile webFile =
						consoleContext
							.files ()
							.get (file);

					if (webFile == null) {

						log.error (
							stringFormat (
								"context %s has no file %s",
								consoleContext.name (),
								file));

						throw new PageNotFoundException ();

					}

					log.debug (
						stringFormat (
							"returning successful resolution"));

					return webFile;

				}

			} catch (ExternalRedirectException exception) {

				throw exception;

			} catch (Exception exception) {

				throw new RuntimeException (
					stringFormat (
						"error initialising context %s",
						consoleContext.name ()),
					exception);

			}

			// process links

			log.debug (
				stringFormat (
					"multiple remaining path parts, assuming link"));

			String link =
				pathParts.next ();

			if (
				! equal (
					link,
					"-")
			) {
				throw new RuntimeException ();
			}

			requestContext.foreignContextPath (
				pathParts.used ());

			String nextPathPart =
				pathParts.next ();

			log.debug (
				stringFormat (
					"context name from path \"%s\"",
					nextPathPart));

			String contextName =
				"link:" + nextPathPart;

			consoleContext =
				consoleContextsByName.get (contextName);

			if (consoleContext == null) {

				log.error (
					stringFormat (
						"context not found \"%s\"",
						contextName));

				throw new PageNotFoundException ();

			}

			ConsoleContext searchConsoleContext =
				consoleContext;

			while (searchConsoleContext.parentContextName () != null) {

				searchConsoleContext =
					consoleContextsByName.get (
						searchConsoleContext.parentContextName ());

			}

			String parentContextTabName =
				stringFormat (
					"%s",
					searchConsoleContext.name ());

			parentContextTab =
				contextTabs.get (
					parentContextTabName);

			if (parentContextTab == null) {

				throw new RuntimeException (
					stringFormat (
						"no parent tab %s",
						parentContextTabName));

			}

			log.debug (
				stringFormat (
					"link complete"));

		}

	}

	private
	String stripLastPath (
			@NonNull String path) {

		int position =
			path.lastIndexOf ('/');

		if (position == -1)
			throw new IllegalArgumentException ();

		return path.substring (0, position);

	}

	@Override
	public
	Provider<Responder> responder (
			@NonNull String responderName,
			boolean required) {

		Provider<Responder> responder =
			responders.get (
				responderName);

		if (responder == null && required) {

			throw new IllegalArgumentException (
				stringFormat (
					"No such responder: %s",
					responderName));

		}

		return responder;

	}

	@Override
	public
	ConsoleContext contextWithParentOfType (
			@NonNull ConsoleContext parentContext,
			@NonNull ConsoleContextType contextType,
			boolean required) {

		List<ConsoleContext> contexts =
			ifNull (
				contextsByParentAndType.get (
					Pair.<String,String>of (
						parentContext.name (),
						contextType.name ())),
				Collections.<ConsoleContext>emptyList ());

		if (contexts.size () > 1) {

			List<String> contextNames =
				new ArrayList<String> ();

			for (ConsoleContext context
					: contexts) {

				contextNames.add (
					context.name ());

			}

			throw new RuntimeException (
				stringFormat (
					"Multiple contexts with parent %s of type %s: %s",
					parentContext.name (),
					contextType.name (),
					joinWithSeparator (
						", ",
						contextNames)));

		}

		if (contexts.isEmpty ()) {

			if (required) {

				throw new RuntimeException (
					stringFormat (
						"No context with parent %s of type %s",
						parentContext.name (),
						contextType.name ()));

			} else {

				return null;

			}

		}

		return contexts.get (0);

	}

	@Override
	public
	ConsoleContext contextWithoutParentOfType (
			@NonNull ConsoleContextType contextType,
			boolean required) {

		List<ConsoleContext> contexts =
			ifNull (
				contextsWithoutParentByType.get (
					contextType.name ()),
				Collections.<ConsoleContext>emptyList ());

		if (contexts.size () > 1) {

			throw new RuntimeException ();

		}

		if (contexts.size () == 0) {

			if (required) {

				throw new RuntimeException (
					stringFormat (
						"No context of type %s without parent",
						contextType.name ()));

			} else {

				return null;

			}

		}

		return contexts.get (0);

	}

	@Override
	public
	ConsoleContextType contextType (
			@NonNull String contextTypeName,
			boolean required) {

		ConsoleContextType contextType =
			contextTypesByName.get (
				contextTypeName);

		if (contextType == null && required) {

			throw new RuntimeException (
				stringFormat (
					"No such context type: %s",
					contextTypeName));

		}

		return contextType;

	}

	@Override
	public
	String resolveLocalFile (
			ConsoleContextStuff contextStuff,
			ConsoleContext consoleContext,
			String localFile) {

		if (localFile.charAt (0) == '/') {

			return joinWithoutSeparator (
				requestContext.applicationPathPrefix (),
				contextStuff.foreignPath (),
				contextStuff.substitutePlaceholders (
					localFile));

		} else if (localFile.startsWith ("type:")) {

			String targetContextTypeName =
				localFile.substring (
					"type:".length ());

			ConsoleContextType targetContextType =
				contextType (
					targetContextTypeName,
					true);

			// try and resolve target context

			ConsoleContext targetContext =
				null;

			ConsoleContext searchContext =
				consoleContext;

			while (searchContext != null) {

				targetContext =
					contextWithParentOfType (
						searchContext,
						targetContextType,
						false);

				if (targetContext != null)
					break;

				searchContext =
					isNotNull (
							searchContext.parentContextName ())
						? context (
							searchContext.parentContextName (),
							true)
						: null;

			}

			if (targetContext == null) {

				throw new RuntimeException (
					stringFormat (
						"No context of type %s with parent %s",
						targetContextType.name (),
						consoleContext.name ()));

			}

			// create url

			return joinWithoutSeparator (
				requestContext.applicationPathPrefix (),
				contextStuff.foreignPath (),
				targetContext.pathPrefix (),
				consoleContext.localPathForStuff (
					contextStuff));

		} else {

			return joinWithoutSeparator (
				requestContext.applicationPathPrefix (),
				contextStuff.foreignPath (),
				consoleContext.pathPrefix (),
				consoleContext.localPathForStuff (
					contextStuff),
				"/",
				contextStuff.substitutePlaceholders (
					localFile));

		}

	}

	@Override
	public
	ConsoleContext relatedContext (
			ConsoleContext sourceContext,
			ConsoleContextType targetContextType) {

		log.debug (
			stringFormat (
				"Choosing related context"));

		log.debug (
			stringFormat (
				"Target context type: %s",
				targetContextType.name ()));

		// get current context

		log.debug (
			stringFormat (
				"Source context: %s",
				sourceContext.name ()));

		ConsoleContext targetContext;

		if (
			isNotNull (
				sourceContext.parentContextName ())
		) {

			// get parent of current context

			ConsoleContext parentContext =
				context (
					sourceContext.parentContextName (),
					true);

			log.debug (
				stringFormat (
					"Parent context: %s",
					parentContext.name ()));

			// lookup target context based on parent and type

			targetContext =
				contextWithParentOfType (
					parentContext,
					targetContextType,
					true);

		} else {

			log.debug (
				stringFormat (
					"No parent context"));

			// lookup parent-less target context based on type

			targetContext =
				contextWithoutParentOfType (
					targetContextType,
					true);

		}

		log.debug (
			stringFormat (
				"Target context chosen: %s",
				targetContext.name ()));

		return targetContext;

	}

	@Override
	public
	SupervisorConfig supervisorConfig (
			String name) {

		return supervisorConfigs.get (
			name);

	}

}
