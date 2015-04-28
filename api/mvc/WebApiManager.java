package wbs.platform.api.mvc;

import java.io.IOException;

import javax.inject.Inject;

import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.web.RequestContext;
import wbs.framework.web.RequestHandler;
import wbs.framework.web.Responder;
import wbs.platform.exception.logic.ExceptionLogLogic;

import com.google.common.base.Optional;

@SingletonComponent ("webApiManager")
public
class WebApiManager {

	@Inject
	RequestContext requestContext;

	@Inject
	ExceptionLogLogic exceptionLogic;

	public
	class WebApiActionRequestHandler
		implements RequestHandler {

		final
		WebApiAction action;

		public
		WebApiActionRequestHandler (
				WebApiAction action) {

			this.action =
				action;

		}

		@Override
		public
		void handle ()
			throws IOException {

			try {

				Responder responder =
					action.go ();

				if (responder != null) {
					responder.execute ();
					return;
				}

			} catch (Exception exception) {

				exceptionLogic.logThrowable (
					"webapi",
					requestContext.requestUri (),
					exception,
					Optional.<Integer>absent (),
					false);

			}

			Responder responder =
				action.makeFallbackResponder ()
					.get ();

			responder.execute ();

		}

	}

	public
	RequestHandler makeWebApiActionRequestHandler (
			WebApiAction action) {

		return new WebApiActionRequestHandler (
			action);

	}

}
