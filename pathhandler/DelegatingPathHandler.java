package wbs.web.pathhandler;

import static wbs.utils.string.StringUtils.joinWithoutSeparator;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j;

import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.logging.TaskLogger;
import wbs.web.file.WebFile;
import wbs.web.responder.WebModule;

/**
 * Implementation of PathHandler which delegates to other PathHandlers or
 * WebFiles based on simple string mappings.
 */
@Log4j
@PrototypeComponent ("delegatingPathHandler")
public
class DelegatingPathHandler
	implements PathHandler {

	// singleton dependencies

	@SingletonDependency
	Map <String, WebModule> servletModules;

	// properties

	@Getter @Setter
	Map <String, PathHandler> paths =
		new HashMap<> ();

	@Getter @Setter
	Map <String, WebFile> files =
		new HashMap<> ();

	// life cycle

	@NormalLifecycleSetup
	public
	void afterPropertiesSet () {

		Map <String, String> pathDeclaredByModule =
			new HashMap<> ();

		Map <String, String> fileDeclaredByModule =
			new HashMap<> ();

		// for each one...

		for (
			Map.Entry <String, WebModule> servletModuleEntry
				: servletModules.entrySet ()
		) {

			String servletModuleName =
				servletModuleEntry.getKey ();

			WebModule servletModule =
				servletModuleEntry.getValue ();

			// import all its paths

			Map <String, ? extends PathHandler> modulePaths =
				servletModule.paths ();

			if (modulePaths != null) {

				for (
					Map.Entry <String, ? extends PathHandler> modulePathEntry
						: modulePaths.entrySet ()
				) {

					String modulePathName =
						modulePathEntry.getKey ();

					PathHandler modulePathHandler =
						modulePathEntry.getValue ();

					if (
						pathDeclaredByModule.containsKey (
							modulePathName)
					) {

						throw new RuntimeException (
							stringFormat (
							"Duplicated path '%s' (in %s and %s)",
							modulePathName,
							pathDeclaredByModule.get (
								modulePathName),
							servletModuleName));

					}

					pathDeclaredByModule.put (
						modulePathName,
						servletModuleName);

					log.debug (
						"Adding path " + modulePathName);

					paths.put (
						modulePathName,
						modulePathHandler);

				}

			}

			// import all its files

			Map <String, ? extends WebFile> moduleFiles =
				servletModule.files ();

			if (moduleFiles != null) {

				for (
					Map.Entry <String, ? extends WebFile> moduleFileEntry
						: moduleFiles.entrySet ()
				) {

					String moduleFileName =
						moduleFileEntry.getKey ();

					if (
						fileDeclaredByModule.containsKey (
							moduleFileName)
					) {

						throw new RuntimeException (
							stringFormat (
							"Duplicated file '%s' (in %s and %s)",
							moduleFileName,
							fileDeclaredByModule.get (
								moduleFileName),
							servletModuleName));

					}

					fileDeclaredByModule.put (
						moduleFileName,
						servletModuleName);

					log.debug (
						"Adding file " + moduleFileName);

					files.put (
						moduleFileName,
						moduleFileEntry.getValue ());

				}

			}

		}

	}

	@Override
	public
	WebFile processPath (
			@NonNull TaskLogger taskLogger,
			@NonNull String path)
		throws ServletException {

		log.debug (
			stringFormat (
				"processPath \"%s\"",
				path));

		// strip any trailing '/'

		if (path.endsWith ("/")) {

			path =
				path.substring (
					0,
					path.length () - 1);

		}

		// check for a file with the exact path

		if (files != null) {

			WebFile webFile =
				files.get (path);

			if (webFile != null)
				return webFile;

		}

		// ok, look for a handler, and keep stripping off bits until we find one

		if (paths != null) {

			String remain = "";

			while (true) {

				PathHandler pathHandler =
					paths.get (path);

				if (pathHandler != null) {

					return pathHandler.processPath (
						taskLogger,
						remain);

				}

				int slashPosition =
					path.lastIndexOf ('/');

				if (slashPosition == 0)
					return null;

				if (slashPosition == -1)
					return null;

				remain =
					joinWithoutSeparator (
						path.substring (
							slashPosition),
						remain);

				path =
					path.substring (
						0,
						slashPosition);

			}

		}

		return null;

	}

}
