
$(function() {
	$("#submit").click(function() {
		submitProject();
	});

	var options = ["testing", "logging", "language", "license"];
	// Fill in each option
	$.get("/project/options?values=" + options.join(","), function(data) {
		for (var i = 0; i < options.length; i++) {
			addValues(options[i], data[options[i]]);
		}
	});
});

/**
 * Collects all the data from the form and submits it using POST /project
 */
function submitProject() {
	var data = getProjectProperties()
	$.ajax({
		type: "POST",
		url: "/project",
		data: data,
		contentType: "application/x-www-form-urlencoded",
		success: function(data) {
			watchForDownload(data)
		},
		error: function(jqXHR) {
			handleSubmissionError(jqXHR)
		}
	})
}

/**
 * Acts based on the project's status. If it is ready, then the project is downloaded. If "errored", then
 * handleBuildError() will be invoked. If "enqueued" or "building", then a timeout will be created to call this method
 * again with updated information
 *
 * @param project A project model
 */
function watchForDownload(project) {
	switch (project.status) {
		case "ready":
			download(project);
			return;
		case "errored":
			handleBuildError();
			return;
		case "enqueued":
		case "building":
			console.log("Creating timeout for 1sec in advance");
			setTimeout(function() {
				console.log("Submitting AJAX");
				$.get("/project/" + project.id, function(updatedProject) {
					watchForDownload(updatedProject)
				}, 1000)
			})
	}
}

/**
 * Downloads the given project
 */
function download(project) {
	window.location.href = "/project/" + project.id + "/download";
}

/**
 * Handles an event where project's status is "errored"
 */
function handleBuildError() {
	// TODO: Error handling
	console.log("Build errored")
}

/**
 * Handles any errors returned from POST /project
 * @param jqXHR The XHR used to send this AJAX request
 */
function handleSubmissionError(jqXHR) {
	// TODO: Error handling
	console.log(jqXHR);
	console.error(jqXHR.responseJSON.why)
}

/**
 * Gets the keys and values from every div.property's input or select attribute. If a <select> is present and multiple
 * options are selected, the selected values will be combined into a comma separated list.
 *
 * @returns {string}
 */
function getProjectProperties() {
	var postData = {};
	$("div.property > input").each(function() {
		// Assing map[id] = input value
		postData[$(this).attr("id")] = $(this).val()
	});
	$("div.property > select").each(function() {
		// Assign map[id] = comma separated list of selected options
		postData[$(this).attr("id")] = $(this).children("option:selected").map(function() {
			return this.value
		}).get().join(",");
	});
	return postData;
}

/**
 * Adds the given values of the option to a &lt;select&gt; whose ID is the given option.
 *
 * @param option The ID of the &lt;select&gt; to add the data to
 * @param data A map of options, where the keys are the constants and values are their more human readable counterparts
 */
function addValues(option, data) {
    var elem = $("select[id=" + option + "]");
    $.each(data, function(key, value) {
        elem.append($("<option></option>")
            .attr("value", key)
            .text(value));
    });
}
