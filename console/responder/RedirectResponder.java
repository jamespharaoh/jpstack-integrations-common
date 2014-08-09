package wbs.platform.console.responder;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.web.Responder;
import wbs.platform.console.request.ConsoleRequestContext;

@Accessors (fluent = true)
@PrototypeComponent ("redirectResponder")
public
class RedirectResponder
	implements
		Provider<Responder>,
		Responder {

	// dependencies

	@Inject
	ConsoleRequestContext requestContext;

	// properties

	@Getter @Setter
	String targetUrl;

	// implementation

	@Override
	public
	void execute ()
		throws IOException {

		requestContext.sendRedirect (
			targetUrl);

	}

	@Override
	public
	Responder get () {

		return this;

	}

}
