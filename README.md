```java
               _//  _//                                                                 
             _/     _//                    _/                                           
    _//    _/_/ _/_/_/ _/   _//    _/ _///    _/// _// _//    _//       _//      _//    
  _//  _//   _//    _//   _/   _//  _//   _//  _//  _/  _// _//  _//  _//  _// _/   _// 
 _//   _//   _//    _//  _///// _// _//   _//  _//  _/  _//_//   _// _//   _//_///// _//
 _//   _//   _//    _//  _/         _//   _//  _//  _/  _//_//   _//  _//  _//_/        
   _// _///  _//     _//   _////   _///   _// _///  _/  _//  _// _///     _//   _////   
                                                                       _//
```
# Nostr Public-Key-reputation relay mesh network  

AfterImage is a PublicKey-Reputation <abbr title="AfterImage relays can be made aware of other AfterImage relays, aggregating and transmitting reputations back to the mesh.">nostr relay mesh-network</abbr>.  It listens to nostr-relays for PublicKey [NIP-58 Badge Award](https://github.com/nostr-protocol/nips/blob/master/58.md#badge-award-event) (vote / reputation-input / etc) events, then calculates and updates a reputation for every PublicKey it finds, yielding PublicKey-Reputations available for Nostr clients to query.

Reputations are calculated based on customizable reputation formula/function provided by the AfterImage operator.

### Core functions:
- vote/reputation-input recipient-PublicKey event listener 
- reputation calculator 
- reputation relay/server
- reputation mesh-network / reputation aggregator

### Secondary function:
- extensible reputation-authority framework

----

### Normal/Production Mode Instructions:
#### Confirm docker requirements

    $ docker --version
>     Docker version 27.5.0
    $ docker compose version
>     Docker Compose version v2.32.4

(Download links for the above)
- [Docker](https://hub.docker.com/_/docker) 27.5.0
- [Docker Compose](https://docs.docker.com/compose/install/) v2.32.4

_(note: Confirmed compatible with Docker 27.0.3 and Docker Compose version v2.28.1 or higher.  Earlier versions are at the liability of the developer/administrator)_

----

#### Download Superconductor Docker Image from [hub.docker](https://hub.docker.com/repository/docker/avlo/afterimage-app/tags)
    $ docker pull avlo/afterimage-nostr-reputation-relay:1.0.0

----

#### Configure AfterImage security level, 3 options:

<details>
  <summary>Highest | SSL Certificate (WSS/HTTPS)</summary>
  <ul>
    <li><a href="https://www.websitebuilderexpert.com/building-websites/how-to-get-an-ssl-certificate/">Obtain</a> an SSL certificate</li>
    <li><a href="https://www.baeldung.com/java-import-cer-certificate-into-keystore">Install</a> the certificate</li>
    <li>Download <a href="src/main/resources/application-prod_wss.properties.properties">application-prod_wss.properties</a> file & configure <a href="src/main/resources/application-prod_wss.properties.properties?plain=1#L6,8,L11-L15"> SSL settings</a></li>
    <li>Download <a href="docker-compose-prod_wss.yml">docker-compose-prod_wss.yml</a> file <i>(and optionally <a href="docker-compose-prod_wss.yml?plain=1#L10,32,L36-L37">edit relevant parameters</a> as applicable)</i></li>
  </ul>
</details>

<details>
  <summary>Medium | Self-Signed Certificate (WSS/HTTPS)</summary>
  <ul>
    <li><a href="https://www.baeldung.com/openssl-self-signed-cert">Create </a>a Self-Signed Certificate</li>
	<li><a href="https://www.baeldung.com/java-import-cer-certificate-into-keystore">Install</a> the certificate</li>
	<li>Download <a href="src/main/resources/application-prod_wss.properties.properties">application-prod_wss.properties</a> file & configure <a href="src/main/resources/application-prod_wss.properties.properties?plain=1#L6,8,L11-L15"> SSL settings</a></li>
    <li>Download <a href="docker-compose-prod_wss.yml">docker-compose-prod_wss.yml</a> file <i>(and optionally <a href="docker-compose-prod_wss.yml?plain=1#L10,32,L36-L37">edit relevant parameters</a> as applicable)</i></li>
  </ul>
</details> 

<details>
  <summary>Lowest | Non-secure / Non-encrypted (WS/HTTP)</summary>
  <ul>
    <li>Security-related configuration(s) not required</li>
    <li>Download <a href="docker-compose-prod_ws.yml">docker-compose-prod_ws.yml</a> file <i>(and optionally <a href="docker-compose-prod_ws.yml?plain=1#L10,32,L36-L37">edit relevant parameters</a> as applicable)</i></li>
  </ul>
</details>

----

#### Run AfterImage

<details>
  <summary>WSS/HTTPS</summary>  

run without logging:

    docker compose -f docker-compose-prod_wss.yml up 

run with container logging displayed to console:  

    docker compose -f docker-compose-prod_wss.yml up --abort-on-container-failure --attach-dependencies

run with docker logging displayed to console:  

    docker compose -f docker-compose-prod_wss.yml up -d && dcls | grep 'afterimage-app' | awk '{print $1}' | xargs docker logs -f
</details> 

<details>
  <summary>WS/HTTP</summary>  

run without logging:

    docker compose -f docker-compose-prod_ws.yml up 

run with container logging displayed to console:

    docker compose -f docker-compose-prod_ws.yml up --abort-on-container-failure --attach-dependencies

run with docker logging displayed to console:

    docker compose -f docker-compose-prod_ws.yml up -d && dcls | grep 'afterimage-app' | awk '{print $1}' | xargs docker logs -f
</details> 

----

##### Stop AfterImage

<details>
  <summary>WSS/HTTPS</summary>

    docker compose -f docker-compose-prod_wss.yml stop afterimage-app afterimage-db
</details> 

<details>
  <summary>WS/HTTP</summary>  

    docker compose -f docker-compose-prod_ws.yml stop afterimage-app afterimage-db
</details>

----  

##### Remove AfterImage docker containers

<details>
  <summary>WSS/HTTPS</summary>

    docker compose -f docker-compose-prod_wss.yml down --remove-orphans
</details> 

<details>
  <summary>WS/HTTP</summary>  

    docker compose -f docker-compose-prod_ws.yml down --remove-orphans
</details>

<hr style="border:2px solid grey">

### Providing Nostr relays for reputation-input events (by AfterImage)
Upon receiving a [NIP-51 Search Relays](https://github.com/nostr-protocol/nips/blob/master/51.md#standard-lists) JSON event containing one or more nostr-relays:


```java
{
  ...
  "kind": 10007,
  "tags": [
    ["relay", "<NOSTR_RELAY_1_URL>"],
    ["relay", "<NOSTR_RELAY_2_URL>"],
    ...
    ["relay", "<NOSTR_RELAY_N_URL>"],
  ]        
  ...
}
```
AfterImage monitors those relays for [NIP-58 Badge Award](https://github.com/nostr-protocol/nips/blob/master/58.md#badge-award-event) events as input to its reputation function.    

<hr style="border:2px solid grey">

### Public-Key-Reputation submission request (by Nostr Clients)

Upon receiving a Nostr [NIP-58 Badge Award](https://github.com/nostr-protocol/nips/blob/master/58.md#badge-award-event) JSON request with the following request filters:

```java
[
  "REQ",
  "<SUBSCRIPTION_ID>", 
  {
    "kinds":[8]
    '#p': ["<REPUTATION_RECIPIENT_PUBKEY>"],
    '#d': ['REPUTATION']
  }
]
```

AfterImage then applies a [user-provided reputation calculation](src/main/java/com/prosilion/afterimage/calculator/UnitReputationCalculator.java), returning a _**REPUTATION_RECIPIENT_PUBKEY**_'s cumulative reputation score (in [NIP-58 Badge Definition Event](https://github.com/nostr-protocol/nips/blob/master/58.md#badge-definition-event) format) as follows:

```java
{
  ...
  "kind": 8,
  "pubkey": "<AFTERIMAGE_RELAY_PUBKEY>",
  "tags": [
    ["a", "30009:<AFTERIMAGE_RELAY_PUBKEY>:REPUTATION"],
    ["p", "<REPUTATION_RECIPIENT_PUBKEY>"]
  ]
  "content": "<reputation_score>"
  ...
}
```
<hr style="border:2px solid grey">



### Reputation permanence
Afterimage reputation events adhere to [NIP-58 Award Badge Specification](https://github.com/nostr-protocol/nips/blob/master/58.md#badges)- and a such- are immutable and non-transferable.

<hr style="border:2px solid grey">

#### [Development Mode Instructions](DEVELOPMENT.md)
