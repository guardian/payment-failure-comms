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
