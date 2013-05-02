function memoryFormatter(v, axis) {
	return (v/(1024*1024)).toFixed(axis.tickDecimals) + " MB";
}

function cpuUsageFormatter(v, axis) {
	return v.toFixed(axis.tickDecimals) + " %";
}


function initStatistics(cpuUsageValues, memoryUsedValues, durationsFor1clickSeriesJson, nbUsersValues, durationsFor1clickMedianJson) {
	var series = [];
	// adding durations
	for (var i = 0 ; i < durationsFor1clickSeriesJson.length ; i++) {
		series.push(durationsFor1clickSeriesJson[i]);
	}
	series.push({ data: memoryUsedValues, label: "Memory", yaxis: 3, color: '#0E660E'});
	series.push({ data: cpuUsageValues, label: "CPU", yaxis: 2, color: '#193C80'});
	series.push({ data: nbUsersValues, label: "Users", yaxis: 4, color: '#888888'});
	series.push({ data: durationsFor1clickMedianJson, label: "Median", yaxis: 5, color: '#000000'});
	
	plot = $.plot($("#placeholder"), series,
   		{
			xaxes: [{ mode: "time", timezone: "browser"}],
			yaxes: [
			        {axisLabel: 'nb of actions', zoomRange: false, panRange: false},	// durationsFor1clickSeriesJson
			        {zoomRange: false, panRange: false, position: 'right', tickFormatter: cpuUsageFormatter},	// CPU
			        {zoomRange: false, panRange: false, position: 'right', tickFormatter: memoryFormatter},		// Memory
			        {axisLabel: 'nb of users', zoomRange: false, panRange: false},		// Users
			        {axisLabel: 'milliseconds', zoomRange: false, panRange: false}		// Median
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