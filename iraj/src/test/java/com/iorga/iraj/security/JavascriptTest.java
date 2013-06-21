package com.iorga.iraj.security;


public class JavascriptTest {
/* with
 			<dependency>
				<groupId>org.mozilla</groupId>
				<artifactId>rhino</artifactId>
				<version>1.7R4</version>
				<scope>test</scope>
			</dependency>
 */
	//@Test
//	public void testJavascript() throws FileNotFoundException, IOException {
//		// from http://www.informit.com/guides/content.aspx?g=java&seqNum=562
//		final Context context = Context.enter();
//		final ScriptableObject scope = context.initStandardObjects();
//
//		context.evaluateReader(scope, new InputStreamReader(new FileInputStream("src/main/resources/META-INF/resources/js/crypto-js/md5.js")), "md5.js", 0, null);
//		context.evaluateReader(scope, new InputStreamReader(new FileInputStream("src/main/resources/META-INF/resources/js/crypto-js/hmac-sha1.js")), "hmac-sha1.js", 0, null);
//		context.evaluateReader(scope, new InputStreamReader(new FileInputStream("src/main/resources/META-INF/resources/js/crypto-js/enc-base64-min.js")), "enc-base64-min.js", 0, null);
//		context.evaluateReader(scope, new InputStreamReader(new FileInputStream("src/main/resources/META-INF/resources/js/security-utils.js")), "security-utils.js", 0, null);
//
//		final String accessKeyId = SecurityUtils.generateAccessKeyId();
//		final String test = "" +
//				"var request = {" +
//				"	method: 'GET'," +
//				"	body: 'Body Test'," +
//				"	headers: {" +
//				"		'Content-Type': 'text/plain'," +
//				"		'Date': 'Mon, 22 Apr 2013 00:00:00 GMT'," +
//				"		'X-IRAJ-Date': 'Thu, 01 Jan 1970 00:00:00 GMT'" +
//				"	}," +
//				"	ressource: '/'" +
//				"};" +
//				"securityUtils.addAuthenticationHeader('"+accessKeyId+"', 'iLKJ8zhzU/5eEZFKeQ5bP+piXQ/JQr4+QKbORZP0', request);" +
//				"request;";
//		final Object request = context.evaluateString(scope, test, "test.js", 0, null);
//		Assert.assertEquals("IWS "+accessKeyId+":fWUiX2xF1+oSDIv7m+3cbo8Ve88=", ((NativeObject)((NativeObject)request).get("headers")).get("Authorization"));
//	}
}
