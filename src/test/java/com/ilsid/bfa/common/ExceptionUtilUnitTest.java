package com.ilsid.bfa.common;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

public class ExceptionUtilUnitTest extends BaseUnitTestCase {

	private static final String EXCEPTION_MSG = "Test message";

	private static final String CAUSEDBY_STR = "   Caused by: ";

	public void testGetExceptionMessageChain_noCauses() {
		Exception e = createException(EXCEPTION_MSG, new String[] {});
		assertEquals(EXCEPTION_MSG, ExceptionUtil.getExceptionMessageChain(e));
	}

	@Test
	public void exceptionMessageChainWithOneMessageIsGenerated() {
		String causeMsg = "Cause message";
		Exception e = createException(EXCEPTION_MSG, new String[] { causeMsg });

		StringBuilder msgChain = new StringBuilder();
		msgChain.append(EXCEPTION_MSG).append(StringUtils.LF).append(CAUSEDBY_STR).append(causeMsg);

		assertEquals(msgChain.toString(), ExceptionUtil.getExceptionMessageChain(e));
	}

	@Test
	public void exceptionMessageChainWithSeveralMessagesIsGenerated() {
		String causeMsg1 = "Cause message 1";
		String causeMsg2 = "Cause message 2";
		String causeMsg3 = "Cause message 3";

		Exception e = createException(EXCEPTION_MSG, new String[] { causeMsg1, causeMsg2, causeMsg3 });

		StringBuilder msgChain = new StringBuilder();
		msgChain.append(EXCEPTION_MSG).append(StringUtils.LF).append(CAUSEDBY_STR).append(causeMsg1)
				.append(StringUtils.LF).append(CAUSEDBY_STR).append(causeMsg2).append(StringUtils.LF)
				.append(CAUSEDBY_STR).append(causeMsg3);

		assertEquals(msgChain.toString(), ExceptionUtil.getExceptionMessageChain(e));
	}

	@Test
	public void messagesAreConvertedToException() {
		String expectedErrMessage = "Msg01" + StringUtils.LF + "Msg02" + StringUtils.LF + "Msg03" + StringUtils.LF;
		@SuppressWarnings("serial")
		List<String> messages = new LinkedList<String>() {
			{
				add("Msg01");
				add("Msg02");
				add("Msg03");
			}
		};

		final Exception excp = ExceptionUtil.toException(messages);
		assertEquals(expectedErrMessage, excp.getMessage());
		assertNull(excp.getCause());
	}

	private Exception createException(String msg, String[] causeMessages) {
		Exception cause = null;
		Exception lowLevelExcp = null;
		for (int i = causeMessages.length - 1; i > -1; i--) {
			String curMsg = causeMessages[i];
			cause = new Exception(curMsg, lowLevelExcp);
			lowLevelExcp = cause;
		}

		return new Exception(msg, cause);
	}

}
