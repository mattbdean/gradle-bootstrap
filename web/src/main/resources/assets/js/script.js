
$(function() {
	$("#submit").click(function() {
		clearErrors();
		submitProject();
	});


	// Fill in each option
	$.get("/project/options", function(data) {
		var handledDefaults = [];
		for (var option in data.enums) {
			if (!data.enums.hasOwnProperty(option)) {
				continue;
			}
			addValues(option, data.enums[option], data.defaults[option]);
			handledDefaults.push(option)
		}
		// Apply some style to <select>
		$("select").chosen();

		for (option in data.defaults) {
			if (!data.defaults.hasOwnProperty(option)) {
				continue;
			}
			// We haven't handled this default value yet, assign it to an input
			if ($.inArray(option, handledDefaults) === -1) {
				$("input[id=" + option + "]").val(data.defaults[option])
			}
		}
	});
});

/**
 * Collects all the data from the form and submits it using POST /project
 */
function submitProject() {
	$.ajax({
		type: "POST",
		url: "/project",
		data: getProjectProperties(),
		contentType: "application/x-www-form-urlencoded",
		success: function(data) {
			watchForDownload(data)
		},
		error: function(jqXHR) {
			handleSubmissionError(jqXHR)
		}
	})
}

function clearErrors() {
	$("p.error").remove();
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
 * @param jqXHR The XMLHttpRequest used to send this AJAX request
 */
function handleSubmissionError(jqXHR) {
	// TODO: Error handling
	console.log(jqXHR);
	var paramName = jqXHR.responseJSON.param.name;
	var why = jqXHR.responseJSON.websiteWhy;
	$("div.property[data-name=" + paramName + "]").first().append(makeErrorHtml(why))
}

function makeErrorHtml(why) {
	return "<p class=\"error\">" + why + "</p>"
}

/**
 * Gets the keys and values from every div.property's input or select attribute. If a <select> is present and multiple
 * options are selected, the selected values will be combined into a comma separated list.
 *
 * @returns {object}
 */
function getProjectProperties() {
	var postData = {};
	$("div.property > input").each(function() {
		// Assigning map[id] = input value
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
 * @param values A map of code names to human readable names
 * @param defaultValue The default value for this element
 */
function addValues(option, values, defaultValue) {
    var elem = $("select[id=" + option + "]");
    $.each(values, function(key, value) {
        elem.append($("<option></option>")
            .attr("value", key)
            .text(value));
    });
	// Set default value
	elem.val(defaultValue)
}
