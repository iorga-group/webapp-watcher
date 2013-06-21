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