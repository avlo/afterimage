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

### Development Mode

- [SOLID](https://www.digitalocean.com/community/conceptual-articles/s-o-l-i-d-the-first-five-principles-of-object-oriented-design) engineering principles.  Simple.  Clean.  OO.
  - understandability
  - extensibility / modularization [(_HOW-TO: creating relay event-handlers_)](#adding-newcustom-events-to-afterimage)
  - testing
  - customization


- Dependencies:
  - Java 21
  - Spring [Boot](https://spring.io/projects/spring-boot) 3.3.4
  - Spring [WebSocketSession](https://docs.spring.io/spring-session/reference/guides/boot-websocket.html)  3.3.4
  - [nostr-java-core](https://github.com/avlo/nostr-java-core) (nostr events & tags, event messages, request messages & filters)


- Containerized deployment:
  - [Docker](https://hub.docker.com/_/docker) 27.5.0
  - [Docker Compose](https://docs.docker.com/compose/install/) v2.32.4

----

### Requirements

    $ java -version

>     java version "21.0.5" 2024-10-15 LTS
>     Java(TM) SE Runtime Environment (build 21.0.5+9-LTS-239)
>     Java HotSpot(TM) 64-Bit Server VM (build 21.0.5+9-LTS-239, mixed mode, sharing)

    $ mvn -version
>     Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
>     Java version: 21.0.5, vendor: Oracle Corporation

----

### Build AfterImage
    $ cd <your_git_home_dir>
    $ git clone https://github.com/avlo/afterimage
    $ cd afterimage
    $ mvn clean install

----

### JUnit / SpringBootTest AfterImage

AfterImage integration tests require an active nostr-relay connection (configurable via [appication-test.properties](src/test/resources/application-test.properties) file) from which Badge Definitions ([Kind: 3009, NIP-58](https://github.com/nostr-protocol/nips/blob/master/README.md#event-kinds)) exist, as follows:  

```java
{
  ...
  "pubkey": "<BADGE_DEFINITION_SOURCE-PUBKEY>",
  "kind": 30009,
  "tags": [
    ["d","UPVOTE"],
    ...
  ],
  "content": "+1"
}
```
and / or
```java
{
  ...
  "pubkey": "<BADGE_DEFINITION_SOURCE-PUBKEY>",
  "kind": 30009,
  "tags": [
    ["d","DOWNVOTE"],
    ...
  ],
  "content": "-1"
}
```

###### _(see [SuperConductor nostr-relay](https://github.com/avlo/superconductor) for a working implementation)_

and where Badge Award ([Kind: 8, NIP-58](https://github.com/nostr-protocol/nips/blob/master/README.md#event-kinds)) event IdentifierTag exists with (as either [UPVOTE / DOWNVOTE](https://github.com/avlo/superconductor/blob/d2448b2e8dc071ee0a34642785d3384d2ca1a9d6/base/src/main/java/com/prosilion/superconductor/base/service/event/type/SuperconductorKindType.java#L13-L14)) format as follows:

```java
{
  ...
  "kind": 8,
  "pubkey": "AUTHOR_PUBKEY",
  "tags": [
    ["a", "30009:BADGE_DEFINITION_SOURCE-PUBKEY:UPVOTE", "ws://vote-occurence-relay-uri"],
    ["p", "VOTE_RECIPIENT_PUBKEY"]
  ]
  ...
}     
```

and / or
```java
{
  ...
  "kind": 8,
  "pubkey": "AUTHOR_PUBKEY",
  "tags": [
    ["a", "30009:BADGE_DEFINITION_SOURCE-PUBKEY:DOWNVOTE", "ws://vote-occurence-relay-uri"],
    ["p", "VOTE_RECIPIENT_PUBKEY"]
  ]
  ...
}     
```
For AfterImage to then generate [reputation events](src/main/java/com/prosilion/afterimage/enums/AfterimageKindType.java).

See [AfterimageReqThenSuperconductorEventIT](src/test/java/com/prosilion/afterimage/service/reactive/AfterimageReqThenSuperconductorEventIT.java) integration test for additional / implementation details and examples.

----

### Run AfterImage (4 options)

#### 1.  Docker + Docker Compose
##### Confirm minimal docker requirements
    $ docker --version
>     Docker version 27.5.0
    $ docker compose version
>     Docker Compose version v2.32.4

_(note: Confirmed compatible with Docker 27.0.3 and Docker Compose version v2.28.1 or higher.  Earlier versions are at the liability of the developer/administrator)_
##### Dockerize project
Superconductor spring boot docker uses [buildpacks](https://buildpacks.io/) ([preferential over Dockerfile](https://reflectoring.io/spring-boot-docker/))

    $ mvn -N wrapper:wrapper
    $ mvn spring-boot:build-image

(*optionally edit [docker-compose-dev_wss.yml](docker-compose-dev_wss.yml?plain=1#L10,L32,L36-L37) parameters as applicable.*)

##### Start docker containers

<details>
  <summary>WSS/HTTPS</summary>  

run without logging:

    docker compose -f docker-compose-dev_wss.yml up 

run with container logging displayed to console:

    docker compose -f docker-compose-dev_wss.yml up --abort-on-container-failure --attach-dependencies

run with docker logging displayed to console:

    docker compose -f docker-compose-dev_wss.yml up -d && dcls | grep 'afterimage-app' | awk '{print $1}' | xargs docker logs -f
</details> 

<details>
  <summary>WS/HTTP</summary>  

run without logging:

    docker compose -f docker-compose-dev_ws.yml up 

run with container logging displayed to console:

    docker compose -f docker-compose-dev_ws.yml up --abort-on-container-failure --attach-dependencies

run with docker logging displayed to console:

    docker compose -f docker-compose-dev_ws.yml up -d && dcls | grep 'afterimage-app' | awk '{print $1}' | xargs docker logs -f
</details> 

----

##### Stop docker containers

<details>
  <summary>WSS/HTTPS</summary>

    docker compose -f docker-compose-dev_wss.yml stop afterimage afterimage-db
</details> 

<details>
  <summary>WS/HTTP</summary>  

    docker compose -f docker-compose-prod_ws.yml stop afterimage afterimage-db
</details>

----  

##### Remove docker containers

<details>
  <summary>WSS/HTTPS</summary>

    docker compose -f docker-compose-dev_wss.yml down --remove-orphans
</details> 

<details>
  <summary>WS/HTTP</summary>  

    docker compose -f docker-compose-prod_ws.yml down --remove-orphans
</details>  

----

### 2.  Run locally using maven spring-boot:run target

<details>
  <summary>WSS/HTTPS</summary>


    cd <your_git_home_dir>/afterimage
    mvn spring-boot:run -P local_wss
</details> 

<details>
  <summary>WS/HTTP</summary>

    cd <your_git_home_dir>/afterimage
    mvn spring-boot:run -P local_ws
</details>  

----

### 3.  Run locally as executable jar

    $ cd <your_git_home_dir>/afterimage
    $ java -jar target/afterimage-0.0.1.war  

----

### 4.  Run using pre-existing local application-server-container instance

    $ cp <your_git_home_dir>/afterimage/target/afterimage-0.0.1.war <your_container/instance/deployment_directory>

----

### Relay Endpoint for clients

<details>
  <summary>WSS/HTTPS</summary>

    wss://localhost:5556
</details> 

<details>
  <summary>WS/HTTP</summary>  

    ws://localhost:5556
</details>

<hr style="border:2px solid grey">

### Default/embedded H2 DB console (local non-docker development mode): ##

    localhost:5556/h2-console/

*user: sa*  
*password: // blank*

Display all framework table contents (case-sensitive quoted fields/tables when querying):

    select id, pub_key, session_id, challenge from auth;
    select id, concat(left(event_id_string,10), '...') as event_id_string, kind, nip, created_at, concat(left(pub_key,10), '...') as pub_key, content from event;
    select id, event_id, event_tag_id from "event-event_tag-join";
    select id, event_id_string, recommended_relay_url, marker from event_tag;
    select id, event_id, pubkey_id from "event-pubkey_tag-join";
    select id, event_id from deletion_event;
    select id, concat(left(public_key,10), '...') as public_key, main_relay_url, pet_name from pubkey_tag;
    select id, event_id, subject_tag_id from "event-subject_tag-join";
    select id, subject from subject_tag;
    select id, hashtag_tag from hashtag_tag;
    select id, location from geohash_tag;
    select id, identifier from identifier_tag;
    select id, event_id, geohash_tag_id from "event-geohash_tag-join";
    select id, event_id, hash_tag_id from "event-hashtag_tag-join";
    select id, event_id, generic_tag_id from "event-generic_tag-join";
    select id, event_id, identifier_tag_id from "event-identifier_tag-join";
    select id, code from generic_tag;
    select id, generic_tag_id, element_attribute_id from "generic_tag-element_attribute-join";
    select id, name, "value" from element_attribute;
    select id, event_id, price_tag_id from "event-price_tag-join";
    select id, uri from relays_tag;
    select id, event_id, relays_id from "event-relays_tag-join";
    select id, number, currency, frequency from price_tag;

##### (Optional Use) bundled web-client URLs for convenience/dev-testing/etc

http://localhost:5556/api-tests.html <sup>_(nostr **events** web-client)_</sup>

http://localhost:5556/request-test.html <sup>_(nostr **request** web-client)_</sup>
<br>
<hr style="border:2px solid grey">

### Adding new/custom events to AfterImage

For Nostr clients generating canonical Nostr JSON (as defined in [NIP01 spec: Basic protocol flow description, Events, Signatures and Tags](https://nostr-nips.com/nip-01)), AfterImage will automatically recognize those JSON events- including their database storage, retrieval and subscriber notification.  No additional work or customization is necessary.
<br>
<hr style="border:2px solid grey">

### Adding new/custom tags to AfterImage

AfterImage supports any _**generic**_ tags automatically.  Otherwise, if custom tag structure is required, simply implement the [`TagPlugin`](https://github.com/avlo/afterimage/blob/master/src/main/java/com/prosilion/afterimage/dto/TagPlugin.java) interface (an example can be seen [here](https://github.com/avlo/afterimage/blob/master/src/main/java/com/prosilion/afterimage/dto/EventTagPlugin.java)) and your tag will automatically get included by AfterImage after rebuilding and redeploying.

