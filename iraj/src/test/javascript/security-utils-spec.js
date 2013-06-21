describe('the securityUtils object', function() {
	// beforeEach(function () {});
	// afterEach(function () {});
	
	// Specs
	it('must add the authentication header to a simple request', function() {
		var request = {
			method: 'GET',
			body: 'Body Test',
			headers: {
				'Content-Type': 'text/plain',
				'Date': 'Mon, 22 Apr 2013 00:00:00 GMT',
				'X-IRAJ-Date': 'Thu, 01 Jan 1970 00:00:00 GMT'
			},
			resource: '/'
		};
		securityUtils.addAuthorizationHeader('5YP9Z3DVCAHVDZPC0617VT91D', 'iLKJ8zhzU/5eEZFKeQ5bP+piXQ/JQr4+QKbORZP0', request);
		expect(request.headers['Authorization']).toBe('IWS 5YP9Z3DVCAHVDZPC0617VT91D:fWUiX2xF1+oSDIv7m+3cbo8Ve88=');
	});
	it('must add a date field if not exists', function() {
		var request = {
			method: 'GET',
			body: 'Body Test',
			headers: {
				'Content-Type': 'text/plain',
			},
			resource: '/'
		};
		securityUtils.addAuthorizationHeader('5YP9Z3DVCAHVDZPC0617VT91D', 'iLKJ8zhzU/5eEZFKeQ5bP+piXQ/JQr4+QKbORZP0', request);
		expect(request.headers['X-IRAJ-Date']).toBeDefined();
	});
	it('must not take care of the Content-Type case', function() {
		var date = new Date().toUTCString();
		var request1 = {
			method: 'GET',
			body: 'Body Test',
			headers: {
				'Content-Type': 'text/plain;charset=utf-8',
				'X-IRAJ-Date': date
			},
			resource: '/'
		};
		securityUtils.addAuthorizationHeader('5YP9Z3DVCAHVDZPC0617VT91D', 'iLKJ8zhzU/5eEZFKeQ5bP+piXQ/JQr4+QKbORZP0', request1);
		var request2 = {
			method: 'GET',
			body: 'Body Test',
			headers: {
				'Content-Type': 'text/plain;charset=UTF-8',
				'X-IRAJ-Date': date
			},
			resource: '/'
		};
		securityUtils.addAuthorizationHeader('5YP9Z3DVCAHVDZPC0617VT91D', 'iLKJ8zhzU/5eEZFKeQ5bP+piXQ/JQr4+QKbORZP0', request2);
		expect(request1.headers['Authorization']).toEqual(request2.headers['Authorization']);
	})
});