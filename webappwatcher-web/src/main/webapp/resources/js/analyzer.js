function memoryFormatter(v, axis) {
	return (v/(1024*1024)).toFixed(axis.tickDecimals) + " Mo";
}

function cpuUsageFormatter(v, axis) {
	return v.toFixed(axis.tickDecimals) + " %";
}

function initAnalyzer(cpuUsages, memoryUsedList) {
	/*
    var markings = [
                    { color: '#f6f6f6', yaxis: { from: 1 } },
                    { color: '#f6f6f6', yaxis: { to: -1 } },
                    { color: 'green', lineWidth: 2, xaxis: { from: 1343292820699, to: 1343292895085 }, yaxis: { from: -1, to: -1} },
                    { color: '#000', lineWidth: 1, xaxis: { from: 8, to: 8 } }
                ];
    */
	plot = $.plot($("#placeholder"),
		[{ data: cpuUsages, label: "CPU = 000.00 %" },
		 { data: memoryUsedList, label: "Memory = 0000.00 Mo", yaxis: 2 }],
   		{
			xaxes: [{ mode: "time" }],
			yaxes: [{ min: 0, max: 100, // min: -2, max: 100,
			          tickFormatter: cpuUsageFormatter,
			          tickDecimals: 2},
			        { tickFormatter: memoryFormatter,
				      tickDecimals: 2,
				      position: 'right',
				      alignTicksWithAxis: 1}],
			/*
			yaxis: { min: 0, max: 100 },
			*/
			crosshair: { mode : "x" },
			grid: { hoverable: true, autoHighlight: false/*, markings: markings*/ },
			legend: { position: 'nw' }
		}
	);
	// Real time update script
	var legends = $("#placeholder .legendLabel");

	var updateLegendTimeout = null;
	var latestPosition = null;
	
	function updateLegend() {
		updateLegendTimeout = null;
		
		var pos = latestPosition;
		
		var axes = plot.getAxes();
		if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max ||
			pos.y < axes.yaxis.min || pos.y > axes.yaxis.max)
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

			legends.eq(i).text(series.label.replace(/=.*/, "= " + series.yaxis.tickFormatter(y, series.yaxis)));
		}
	}
	
	$("#placeholder").bind("plothover",  function (event, pos, item) {
		latestPosition = pos;
		if (!updateLegendTimeout)
			updateLegendTimeout = setTimeout(updateLegend, 50);
	});
}