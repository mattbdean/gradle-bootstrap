
$(function() {
	$("#submit").click(function() {
		var data = getProjectProperties()
		$.ajax({
			type: "POST",
			url: "/project",
			data: data,
			contentType: "application/x-www-form-urlencoded",
			success: function(data) {
				console.log(data)
			},
			error: function(jqXHR) {
				console.log(jqXHR)
			}
			// TODO: Error handling
		})
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
