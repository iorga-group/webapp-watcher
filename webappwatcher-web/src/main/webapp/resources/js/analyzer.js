function memoryFormatter(v, axis) {
	return (v/(1024*1024)).toFixed(axis.tickDecimals) + " MB";
}

function cpuUsageFormatter(v, axis) {
	return v.toFixed(axis.tickDecimals) + " %";
}

function initAnalyzer(cpuUsageValues, memoryUsedValues, markings, firstEventLogTime, lastEventLogTime, maxMemoryUsed) {
	plot = $.plot($("#placeholder"),
		[{ data: memoryUsedValues, label: "Memory = 0000.00 MB", yaxis: 2 },
		 { data: cpuUsageValues, label: "CPU = 000.00 %" }],
   		{
			xaxes: [{ mode: "time",
			          min: firstEventLogTime, max: lastEventLogTime,
			          zoomRange: [1, lastEventLogTime-firstEventLogTime],
			          panRange: [firstEventLogTime, lastEventLogTime]}],
			yaxes: [{ min: -20, max: 100,
			          tickFormatter: cpuUsageFormatter,
			          tickDecimals: 2,
			          zoomRange: false,
			          panRange: false},
			        { max: maxMemoryUsed,
			          tickFormatter: memoryFormatter,
				      tickDecimals: 2,
				      position: 'right',
				      alignTicksWithAxis: 1,
			          zoomRange: false,
			          panRange: false}],
			crosshair: { mode : "x" },
			grid: { hoverable: true, clickable: true, autoHighlight: false, markings: markings },
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
	
	$("#placeholder").bind("plotclick",  function (event, pos, item) {
		$("#selectedTime").val(parseInt(pos.x));
		updateInfoPanel();
	});
}