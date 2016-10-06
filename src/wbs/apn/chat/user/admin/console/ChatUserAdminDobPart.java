package wbs.apn.chat.user.admin.console;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.web.HtmlBlockUtils.htmlParagraphClose;
import static wbs.utils.web.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.utils.web.HtmlFormUtils.htmlFormClose;
import static wbs.utils.web.HtmlFormUtils.htmlFormOpenPostAction;

import wbs.apn.chat.user.core.console.ChatUserConsoleHelper;
import wbs.apn.chat.user.core.model.ChatUserRec;
import wbs.console.part.AbstractPagePart;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.utils.time.TimeFormatter;

@PrototypeComponent ("chatUserAdminDobPart")
public
class ChatUserAdminDobPart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ChatUserConsoleHelper chatUserHelper;

	@SingletonDependency
	TimeFormatter timeFormatter;

	// state

	ChatUserRec chatUser;

	// implementation

	@Override
	public
	void prepare () {

		chatUser =
			chatUserHelper.findRequired (
				requestContext.stuffInteger (
					"chatUserId"));

	}

	@Override
	public
	void renderHtmlBodyContent () {

		htmlFormOpenPostAction (
			requestContext.resolveLocalUrl (
				"/chatUser.admin.dob"));

		htmlParagraphOpen ();

		formatWriter.writeLineFormat (
			"Date of birth (yyyy-mm-dd)<br>");

		formatWriter.writeLineFormat (
			"<input",
			" type=\"text\"",
			" name=\"dob\"",
			" value=\"%h\"",
			ifNull (
				requestContext.getForm ("dob"),
				timeFormatter.dateString (
					chatUser.getDob ()),
				""),
			">");

		htmlParagraphClose ();

		htmlParagraphOpen ();

		formatWriter.writeLineFormat (
			"<input",
			" type=\"submit\"",
			" value=\"update date of birth\"",
			">");

		htmlParagraphClose ();

		htmlFormClose ();

	}

}