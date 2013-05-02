function memoryFormatter(v, axis) {
	return (v/(1024*1024)).toFixed(axis.tickDecimals) + " MB";
}

function cpuUsageFormatter(v, axis) {
	return v.toFixed(axis.tickDecimals) + " %";
}


function initStatistics(cpuUsageValues, memoryUsedValues, durationsFor1clickSeriesJson) {
	var series = [];
	// adding durations
	for (var i = 0 ; i < durationsFor1clickSeriesJson.length ; i++) {
		series.push(durationsFor1clickSeriesJson[i]);
	}
	series.push({ data: cpuUsageValues, label: "CPU", yaxis: 2, color: '#193C80'});
	series.push({ data: memoryUsedValues, label: "Memory", yaxis: 3, color: '#0E660E'});
	plot = $.plot($("#placeholder"), series,
   		{
			xaxes: [{ mode: "time"}],
			yaxes: [
			        {zoomRange: false, panRange: false},	// durationsFor1clickSeriesJson
			        {zoomRange: false, panRange: false, position: 'right', tickFormatter: cpuUsageFormatter},	// CPU
			        {zoomRange: false, panRange: false, position: 'right', tickFormatter: memoryFormatter}		// Memory
			        ],
			crosshair: { mode : "x" },
			grid: { hoverable: true, clickable: true, autoHighlight: false },
			legend: { position: 'nw' },
	        zoom: {
	            interactive: true
	        },
	        pan: {
	            interactive: true
	        }
		}
	);
	
	addUpdateLegendsOnPlotHoverFunction(plot);
}