corda-cli package install -n missionmars-network missionMars.cpb


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
1e9f4d37-cb3b-4238-909d-13f0368fb198

```json
{
  "rpcStartFlowRequest": {
    "clientId": "launchpad-3", 
    "flowName": "net.corda.missionMars.flows.CreateBoardingTicketInitiator", 
    "parameters": { 
      "parametersInJson": "{\"ticketDescription\": \"Space Shuttle 323 - Seat 16B\", \"daysTillLaunch\": \"10\"}" 
    } 
  } 
}
```

```json
{
  "rpcStartFlowRequest": {
    "clientId": "launchpad-4", 
    "flowName": "net.corda.missionMars.flows.RedeemBoardingTicketWithVoucherInitiator", 
    "parameters": { 
      "parametersInJson": "{\"voucherID\": \"1e9f4d37-cb3b-4238-909d-13f0368fb198\", \"holder\": \"C=US, L=New York, O=Peter, OU=INC\"}" 
    } 
  } 
}
```

corda-cli network terminate -n missionmars-network -ry

