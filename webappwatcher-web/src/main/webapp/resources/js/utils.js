function updateLegends(plot, pos, updateLegendTimeout) {
	var legends = $("#"+plot.getPlaceholder().attr('id')+" .legendLabel");
		
	var axes = plot.getAxes();
	
	updateLegendTimeout.timeout = null;
	
	if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max)
		return;

	var i, dataset = plot.getData();
	for (i = 0; i < dataset.length; ++i) {
		var series = dataset[i];
		var datas = series.data;

		// find the nearest points, x-wise, with dichotomic search
		var j = 0, min = 0, max = datas.length - 1, xtarget = pos.x;
		while (min <= max) {
			j = parseInt((max + min) / 2);
			if (datas[j][0] > xtarget) {
				max = j - 1;
			} else if (datas[j][0] < xtarget) {
				min = j + 1;
			} else {
				break;
			}
		}
		j = max; // which will be inferior to min or equal
		
		// now interpolate
		var y, p1 = datas[j], p2 = datas[j + 1];
		if (p1 == null)
			y = p2[1];	// no data on "n", let's take the data on "n + 1"
		else if (p2 == null)
			y = p1[1]; // no data on "n + 1", let's take the data on "n"
		else
			y = p1[1] + (p2[1] - p1[1]) * (xtarget - p1[0]) / (p2[0] - p1[0]);	// interpolation of the 2 values

		if (series.label.indexOf('=') === -1) {
			// no '=' in the label, will append it at the end
			series.label += ' =';
		}
		legends.eq(i).text(series.label.replace(/=.*/, "= " + series.yaxis.tickFormatter(y, series.yaxis)));
	}
}

function removeEqualSignFromLegends(plot) {
	$("#"+plot.getPlaceholder().attr('id')+" .legendLabel").each(function() {
		$(this).text($(this).text().replace(/=.*/, ""));
	});
}

function addUpdateLegendsOnPlotHoverFunction(plot) {
	var updateLegendTimeout = {timeout: null};
	plot.getPlaceholder().bind("plothover",  function (event, pos, item) {
		if (!updateLegendTimeout.timeout) {
			updateLegendTimeout.timeout = setTimeout(function() {
				updateLegends(plot, pos, updateLegendTimeout);
			}, 50);
		}
	});
	plot.getPlaceholder().on('mouseleave', function() {removeEqualSignFromLegends(plot);});
}
