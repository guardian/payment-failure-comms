![CI workflow status](https://github.com/guardian/payment-failure-comms/actions/workflows/ci.yml/badge.svg)

# Payment Failure Comms

This repo contains a lambda that emits payment failure related Custom Events to Braze. It fetches data from Salesforce to determine users entering or exiting payment failure and places that information into Braze using Custom Events.

Its aim is to decouple payment failure comms from payment retries by providing Braze with the information needed for Braze Canvas logic to determine for itself whether to send any user comms or not. Braze will then have the capability to send comms either in direct response to receiving a Custom Event, or from evaluating upon a schedule, the set of Custom Events attributed to a particular user.

## Looking for an overview of the system?

[This document](https://docs.google.com/document/d/1it8R7ijGvAT79fcJmKTaBvo_ih4oJjCvCDO7EKh_R6Y/edit?usp=sharing) describes the architecture at a glance.

## Looking for why the system was designed this way?

Check out the ADR docs [here](https://drive.google.com/drive/folders/1UZ7_HDLACKOkOMos3j_7J4X0OJjwk97v?usp=sharing).

## Deployment

This repo is deployed by a [Github CI workflow](https://github.com/guardian/payment-failure-comms/blob/main/.github/workflows/ci.yml) and Riffraff.  
There is a [paused build](https://teamcity.gutools.co.uk/buildConfiguration/memsub_MembershipAdmin_Build?branch=%3Cdefault%3E&buildTypeTab=overview&mode=builds) in Teamcity, which is due to be deleted on 22 Nov 2021 if there are no problems before then.

## Alarms

If the alarm has title "Action required - payment-failure-comms: failure limit reached for some records", this means at least five payment-failure events within an hour were unable to send to Braze meaning the customer won't get emails about their failed payment. We retry each record five times which is represented by the `PF_Comms_Number_of_Attempts__c` property. The cause of a single failure is rarely a problem with the code, and is rather dependent on the individual account state. For example, many failures are due to the customer deleting their identity account. We have set the threshold of the `FailureLimitReachedAlarm` to five because if it's that high (or higher) it usually suggests a code problem rather than a record-specific issue.

To debug the failures, go to Salesforce and open the Salesforce Inspector. Use the following query to get the failed records:

```
SELECT
       Id,
PF_Comms_Number_of_Attempts__c,
Last_Attempt_Date__c,
Number_of_Failures__c
FROM
           Payment_Failure__c where PF_Comms_Number_of_Attempts__c = 5 AND Last_Attempt_Date__c != null order by Last_Attempt_Date__c desc
```

From here, view the record's data in Salesforce to determine the cause of failure. 
