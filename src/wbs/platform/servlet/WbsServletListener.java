package wbs.platform.servlet;

import static wbs.framework.utils.etc.StringUtils.stringFormat;
import static wbs.framework.utils.etc.StringUtils.stringSplitComma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import wbs.framework.application.context.ApplicationContext;
import wbs.framework.application.tools.ApplicationContextBuilder;
import wbs.framework.application.tools.ThreadLocalProxyComponentFactory;
import wbs.framework.web.RequestContextImplementation;

@Log4j
public
class WbsServletListener
	implements
		ServletContextListener,
		ServletRequestListener {

	ServletContext servletContext;

	ApplicationContext applicationContext;

	@Override
	public
	void contextDestroyed (
			@NonNull ServletContextEvent event) {

		if (applicationContext == null)
			return;

		log.info (
			stringFormat (
				"Destroying application context"));

		applicationContext.close ();

	}

	@Override
	public
	void contextInitialized (
			@NonNull ServletContextEvent event) {

		log.info (
			stringFormat (
				"Initialising application context"));

		servletContext =
			event.getServletContext ();

		String primaryProjectName =
			servletContext.getInitParameter (
				"primaryProjectName");

		String primaryProjectPackageName =
			servletContext.getInitParameter (
				"primaryProjectPackageName");

		String beanDefinitionOutputPath =
			servletContext.getInitParameter (
				"beanDefinitionOutputPath");

		List <String> layerNames =
			stringSplitComma (
				servletContext.getInitParameter (
					"layerNames"));

		applicationContext =
			new ApplicationContextBuilder ()

			.primaryProjectName (
				primaryProjectName)

			.primaryProjectPackageName (
				primaryProjectPackageName)

			.layerNames (
				layerNames)

			.configNames (
				Collections.emptyList ())

			.outputPath (
				beanDefinitionOutputPath)

			.addSingletonComponent (
				"servletContext",
				event.getServletContext ())

			.build ();

		servletContext.setAttribute (
			"wbs-application-context",
			applicationContext);

	}

	@Override
	public
	void requestDestroyed (
			@NonNull ServletRequestEvent event) {

		for (
			String requestBeanName
				: applicationContext.requestComponentNames ()
		) {

			ThreadLocalProxyComponentFactory.Control control =
				(ThreadLocalProxyComponentFactory.Control)
				applicationContext.getComponentRequired (
					requestBeanName,
					Object.class);

			control.threadLocalProxyReset ();

		}

		RequestContextImplementation
			.servletRequestThreadLocal
			.remove ();

	}

	@Override
	public
	void requestInitialized (
			@NonNull ServletRequestEvent event) {

		boolean setServletContext = false;
		boolean setServletRequest = false;

		List <String> setRequestBeanNames =
			new ArrayList<> ();

		boolean success = false;

		try {

			RequestContextImplementation.servletContextThreadLocal.set (
				servletContext);

			setServletContext = true;

			RequestContextImplementation.servletRequestThreadLocal.set (
				(HttpServletRequest)
				event.getServletRequest ());

			setServletRequest = true;

			for (
				String requestBeanName
					: applicationContext.requestComponentNames ()
			) {

				ThreadLocalProxyComponentFactory.Control control =
					(ThreadLocalProxyComponentFactory.Control)
					applicationContext.getComponentRequired (
						requestBeanName,
						Object.class);

				String targetBeanName =
					stringFormat (
						"%sTarget",
						requestBeanName);

				Object targetBean =
					applicationContext.getComponentRequired (
						targetBeanName,
						Object.class);

				control.threadLocalProxySet (
					targetBean);

				setRequestBeanNames.add (
					requestBeanName);

			}

			success = true;

		} finally {

			if (! success) {

				for (
					String requestBeanName
						: setRequestBeanNames
				) {

					ThreadLocalProxyComponentFactory.Control control =
						(ThreadLocalProxyComponentFactory.Control)
						applicationContext.getComponentRequired (
							requestBeanName,
							Object.class);

					control.threadLocalProxyReset ();

				}

				if (setServletRequest) {

					RequestContextImplementation
						.servletRequestThreadLocal
						.remove ();

				}

				if (setServletContext) {

					RequestContextImplementation
						.servletContextThreadLocal
						.remove ();

				}

			}

		}

	}

}
