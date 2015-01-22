
$(function() {
	$("#submit").click(function() {
		clearErrors();
		submitProject();
	});

	$("input[type='checkbox']").change(function() {
		// Refresh dependencies on checkbox click
		refreshDependencies();
	});

	refreshDependencies();

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

function refreshDependencies() {
	var depAttr = "data-depends-on";

	$(".property[" + depAttr + "]").each(function() {
		var name = $(this).attr(depAttr);
		var propertyInput = $("input[id=" + name + "]");
		if (propertyInput.attr("type") !== "checkbox") {
			console.log("Property " + propertyInput + " was not a checkbox");
			return;
		}

		var enabled = false;
		if (propertyInput.is(":checked")) {
			enabled = true;
		}
		$(this).children("input[type=text]").first().prop("disabled", !enabled);
	});
}

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
				});
			}, 1000)
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
	console.log(jqXHR);
	var paramName = jqXHR.responseJSON.param.name;
	var why = jqXHR.responseJSON.websiteWhy;
	appendError(paramName, why)
}

function appendError(name, error) {
	$("div.property[data-name=" + name + "]").first().append(makeErrorHtml(error))
}

function makeErrorHtml(error) {
	return "<p class=\"error\">" + error + "</p>"
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
		if ($(this).prop("disabled")) {
			// Don't send disabled properties
			return;
		}
		var value;
		// Get boolean value of checkbox instead of "on" or "off"
		if ($(this).attr("type") === "checkbox") {
			value = $(this).is(":checked");
		} else {
			value = $(this).val()
		}
		postData[$(this).attr("id")] = value;
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
