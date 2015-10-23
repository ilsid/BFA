package com.ilsid.bfa.script.sample;

import com.ilsid.bfa.script.Script;
import com.ilsid.bfa.script.ScriptException;

/**
 * Demonstration of the scripting concept.  
 * 
 * @author illia.sydorovych
 *
 */
//TODO: complete example
public class SampleScript extends Script {

	protected void doExecute() throws ScriptException {

		DeclareInputVar("Subscriber", "Subscriber");
		DeclareInputVar("Amount", "Decimal");
		DeclareInputVar("AccessType", "AccessType");

		DeclareLocalVar("AmountToReserve", "Decimal");
		DeclareLocalVar("SubscriberBalance", "Number");
		DeclareLocalVar("TempAmount", "Decimal");
		DeclareLocalVar("GlobalConfig", "DefaultConfiguration", GetGlobalVar("GlobalConfig"));
		DeclareLocalVar("CentsCoeff", "Decimal", 100.0);
		DeclareLocalVar("SessionID", "String");
		DeclareLocalVar("ReservedAmount", "Decimal");
		DeclareLocalVar("ReservedLessThanRequested", "Boolean");
		DeclareLocalVar("WasErrorReturned", "Boolean");
		DeclareLocalVar("ErrorCode", "ReserveAmountError");

		SetLocalVar("TempAmount", "Amount");

		if (Equal("GlobalConfig.EnablePrepaidAccounting", AsBoolean("True"), 
			"Is prepaid subscriber?")) {
			
			if (LessOrEqual("Amount", "Subscriber.PrepaidAmount - Subscriber.PrepaidReservedAmount", 
				"Is there enough to cover the charge for subscription?")) {
			
				Action("Round", "TempAmount * CentsCoeff").SetLocalVar("AmountToReserve");
				
				Action("Reserve Amount", 
						"Subscriber.msisdn", 
						"Subscriber.SI", 
						"Subscriber.Address", 
						"AmountToReserve", 
						AsString("EVENT_DRIVEN"), 
						"AccessType")
						.SetLocalVar("SessionID")
						.SetLocalVar("ReservedAmount")
						.SetLocalVar("ReservedLessThanRequested")
						.SetLocalVar("WasErrorReturned")
						.SetLocalVar("ErrorCode");
				
				SubFlow("Prepaid Reservation Event Notification");
			
			} else {
				//TBD
			}

		} else {
			//TBD
		}
		
		//TBD

	}

}
