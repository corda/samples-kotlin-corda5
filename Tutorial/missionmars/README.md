# Corda5 Tutorial Cordapp: Mission Mars

Please refer to the documentation for a detailed walk through of developing and running this App [here](https://docs.r3.com/en/platform/corda/5.0-dev-preview-1/tutorials/building-cordapp/c5-basic-cordapp-intro.html)

Flow #1 input:
```json
{
  "rpcStartFlowRequest": {
    "clientId": "launchpad-1", 
    "flowName": "net.corda.missionMars.flows.CreateAndIssueMarsVoucherInitiator", 
    "parameters": { 
      "parametersInJson": "{\"voucherDesc\": \"Space Shuttle 323\", \"holder\": \"C=US, L=New York, O=Peter, OU=INC\"}" 
    } 
  } 
}
```
Flow #2 input:
```json
{
  "rpcStartFlowRequest": {
    "clientId": "launchpad-2", 
    "flowName": "net.corda.missionMars.flows.CreateBoardingTicketInitiator", 
    "parameters": { 
      "parametersInJson": "{\"ticketDescription\": \"Space Shuttle 323 - Seat 16B\", \"daysUntilLaunch\": \"10\"}" 
    } 
  } 
}
```

[Optional]: If you would like gift the voucher to a different party, run this in PartyB's API interface
```json
{
  "rpcStartFlowRequest": {
    "clientId": "launchpad-3", 
    "flowName": "net.corda.missionMars.flows.GiftVoucherToFriendInitiator", 
    "parameters": {
      "parametersInJson": "{\"voucherID\": \"908e12a5-d43e-4019-8bb6-36571f98935b\", \"holder\": \"C=US, L=San Diego, O=Friend, OU=LLC\"}"
    } 
  } 
}
```
908e12a5-d43e-4019-8bb6-36571f98935b

Flow #3 input: (The voucherID needs to be retrieved from flow #2's output. Use the /flowstarter/flowoutcomeforclientid/{clientid} method, and input [launchpad-3])
```json
{
  "rpcStartFlowRequest": {
    "clientId": "launchpad-4", 
    "flowName": "net.corda.missionMars.flows.RedeemBoardingTicketWithVoucherInitiator", 
    "parameters": { 
      "parametersInJson": "{\"voucherID\": \"908e12a5-d43e-4019-8bb6-36571f98935b\", \"holder\": \"C=US, L=San Diego, O=Friend, OU=LLC\"}" 
    } 
  } 
}
```

How to shut down the app
```
corda-cli network terminate -n missionmars-network -ry
```

