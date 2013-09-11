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
// Dependencies : CryptoJS.MD5 (rollups/md5.js), CryptoJS.SHA1 (rollups/sha1.js), CryptoJS.HmacSHA1 (rollups/hmac-sha1.js), CryptoJS.enc.Base64 (components/enc-base64-min.js)

securityUtils = {
	/*
	 * httpRequestToSign = {
	 * 	 method: // string : 'GET'|'POST'...,
	 *   body: // string of the body of the request
	 *   headers: // {
	 *   	'Content-Type': // string for the contentType,
	 *   	'Date': // date,
	 *   	'X-IRAJ-Date': // forged date} ...
	 *   resource: // PATH-INFO + QUERY-STRING
	 * }
	 */
	computeData: function(httpRequestToSign) {
		var data = '';
		// HTTP method
		data += httpRequestToSign.method + '\n';
		// body MD5
		if (httpRequestToSign.body && httpRequestToSign.body.length > 0) {
			var hash = CryptoJS.MD5(httpRequestToSign.body);
			data += hash.toString(CryptoJS.enc.Hex);
		}
		data += '\n';
		// Content type
		if (httpRequestToSign.headers['Content-Type'] && httpRequestToSign.headers['Content-Type'].length > 0) {
			data += httpRequestToSign.headers['Content-Type'].toLowerCase();
		}
		data += '\n';
		/// Date
		var date = httpRequestToSign.headers['Date'], xirajdate;
		if (httpRequestToSign.headers['X-IRAJ-Date'] && httpRequestToSign.headers['X-IRAJ-Date'].length > 0) {
			xirajdate = httpRequestToSign.headers['X-IRAJ-Date'];
			date = xirajdate;
		}
		data += date + '\n';
		// Handling security additional headers
		//TODO handle this generically, see com.iorga.iraj.security.SecurityUtils.computeData(HttpRequestToSign)
		if (xirajdate) {
			data += 'x-iraj-date:' + xirajdate + '\n';
		}
		data += httpRequestToSign.resource + '\n';
		return data;
	},
	computeDataSignature: function(secretAccessKey, data) {
		var hash = CryptoJS.HmacSHA1(data, secretAccessKey);
		return hash.toString(CryptoJS.enc.Base64);
	},
	computeAuthorizationHeaderValue: function(accessKeyId, secretAccessKey, httpRequestToSign) {
		return 'IWS ' + accessKeyId + ':' + securityUtils.computeDataSignature(secretAccessKey, securityUtils.computeData(httpRequestToSign));
	},
	addAuthorizationHeader: function(accessKeyId, secretAccessKey, httpRequestToSign) {
		if (!httpRequestToSign.headers['X-IRAJ-Date'] && !httpRequestToSign.headers['Date']) {
			httpRequestToSign.headers['X-IRAJ-Date'] = new Date().toUTCString();
		}
		httpRequestToSign.headers['Authorization'] = securityUtils.computeAuthorizationHeaderValue(accessKeyId, secretAccessKey, httpRequestToSign);
	}
}