# Corda5 Tutorial Cordapp: Mission Mars

Please refer to the documentation for detail walk through of developing and running this App at [here](https://docs.r3.com/en/platform/corda/5.0-dev-preview-1/tutorials/building-cordapp/c5-basic-cordapp-intro.html)


Flow #1 input: 
```json
{
  "rpcStartFlowRequest": {
    "clientId": "launchpad-2", 
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
    "clientId": "launchpad-3", 
    "flowName": "net.corda.missionMars.flows.CreateBoardingTicketInitiator", 
    "parameters": { 
      "parametersInJson": "{\"ticketDescription\": \"Space Shuttle 323 - Seat 16B\", \"daysUntilLaunch\": \"10\"}" 
    } 
  } 
}
```
Flow #3 input: (The voucherID need to be retrieved from flow #2's output. Use the /flowstarter/flowoutcomeforclientid/{clientid} method, and input [launchpad-3])
```json
{
  "rpcStartFlowRequest": {
    "clientId": "launchpad-4", 
    "flowName": "net.corda.missionMars.flows.RedeemBoardingTicketWithVoucherInitiator", 
    "parameters": { 
      "parametersInJson": "{\"voucherID\": \"4016e315-931c-4630-8b04-58ad37b8d644\", \"holder\": \"C=US, L=New York, O=Peter, OU=INC\"}" 
    } 
  } 
}
```

How to shut down the app
```
corda-cli network terminate -n missionmars-network -ry
```

