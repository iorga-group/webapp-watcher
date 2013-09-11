/*
 * Copyright (C) 2013 Iorga Group
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/].
 */
'use strict';

angular.module('flotutils-service', [])
	.factory('flotUtilsService', function($timeout) {
		var flotUtilsService = {};
		flotUtilsService.updateLegends = function(plot, pos) {
			var legends = plot.getPlaceholder().find(".legendLabel");
				
			var axes = plot.getAxes();
			
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
		
		flotUtilsService.removeEqualSignFromLegends = function(plot) {
			plot.getPlaceholder().find(".legendLabel").each(function() {
				$(this).text($(this).text().replace(/=.*/, ""));
			});
		}
		
		flotUtilsService.addUpdateLegendsOnPlotHoverFunction = function(plot) {
			var previousTimeoutPromise,
				canMakeAnotherTimeout = true,
				makeAnotherTimeoutPossible = function() {
					canMakeAnotherTimeout = true;
				}
			plot.getPlaceholder().bind("plothover",  function (event, pos, item) {
				if (canMakeAnotherTimeout) {
					canMakeAnotherTimeout = false;
					previousTimeoutPromise = $timeout(function() {
						flotUtilsService.updateLegends(plot, pos);
					}, 50);
					// if success or failure, let's authorize next timeout call
					previousTimeoutPromise.then(makeAnotherTimeoutPossible, makeAnotherTimeoutPossible);
				}
			});
			
			plot.getPlaceholder().on('mouseleave', function() {
				$timeout.cancel(previousTimeoutPromise);
				makeAnotherTimeoutPossible(); // weirdly not called as a errorCallback of the promise...
				flotUtilsService.removeEqualSignFromLegends(plot);
			});
		}
		
		flotUtilsService.memoryFormatter = function(v, axis) {
			return (v/(1024*1024)).toFixed(axis.tickDecimals) + " MB";
		}

		flotUtilsService.cpuUsageFormatter = function(v, axis) {
			return v.toFixed(axis.tickDecimals) + " %";
		}
		
		return flotUtilsService;
	})
;
