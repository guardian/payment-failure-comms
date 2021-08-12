# payment-failure-comms
This repo contains a lambda that emits payment failure related Custom Events to Braze. It fetches data from Salesforce to determine users entering or exiting payment failure and places that information into Braze using Custom Events.

Its aim is to decouple payment failure comms from payment retries by providing Braze with the information needed to trigger these comms.

#### Looking for an overview of the system?
[This document](https://docs.google.com/document/d/1it8R7ijGvAT79fcJmKTaBvo_ih4oJjCvCDO7EKh_R6Y/edit?usp=sharing) describes the architecture at a glance.

#### Looking for why the system was designed this way?
Check out the ADR docs [here](https://drive.google.com/drive/folders/1UZ7_HDLACKOkOMos3j_7J4X0OJjwk97v?usp=sharing).

**Note**: This README will be expanded as the system is developed.
