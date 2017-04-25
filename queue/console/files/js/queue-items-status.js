$(function () {

	wbsStatus.handlerRegister ("queue-items",
	function (data) {

		if (data.total && data.claimed) {

			$("#queue-cell").text ([
				String (data.total),
				" ",
				data.total > 1 ? "items" : "item",
				" queueing and ",
				String (data.claimed),
				" claimed",
			].join (""));

			$("#queue-row").show ();

		} else if (data.total) {

			$("#queue-cell").text ([
				String (data.total),
				" ",
				data.total > 1 ? "items" : "item",
				" queueing",
			].join (""));

			$("#queue-row").show ();

		} else if (data.claimed) {

			$("#queue-cell").text ([
				String (data.total),
				" ",
				data.total > 1 ? "items" : "item",
				" claimed",
			].join (""));

			$("#queue-row").show ();

		} else {

			$("#queue-row").hide ();

		}

	});
});

// ex: noet ts=4 filetype=javascript