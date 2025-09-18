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

AfterImage integration tests require an active nostr-relay connection (configurable via [appication-test.properties](src/test/resources/application-test.properties) file) with the following two (2) requirements:

1 / 2 : [ NIP-58 Badge Definition Event(s)](https://github.com/nostr-protocol/nips/blob/master/58.md#badge-definition-event) exist, as follows

<details><summary>Kind: 30009 - <a href="https://github.com/avlo/superconductor/blob/d2448b2e8dc071ee0a34642785d3384d2ca1a9d6/base/src/main/java/com/prosilion/superconductor/base/service/event/type/SuperconductorKindType.java#L13">Badge Definition UNIT_UPVOTE</a></summary>

```java
{
  ...
  "pubkey": "<BADGE_DEFINITION_SOURCE_RELAY-PUBKEY>",
  "kind": 30009,
  "tags": [
    ["d","UNIT_UPVOTE"],
    ...
  ],
  "content": "+1"
}
```
</details>

<details><summary>Kind: 30009 - <a href="https://github.com/avlo/superconductor/blob/d2448b2e8dc071ee0a34642785d3384d2ca1a9d6/base/src/main/java/com/prosilion/superconductor/base/service/event/type/SuperconductorKindType.java#L14">Badge Definition UNIT_DOWNVOTE</a></summary>

```java
{
  ...
  "pubkey": "<BADGE_DEFINITION_SOURCE_RELAY-PUBKEY>",
  "kind": 30009,
  "tags": [
    ["d","UNIT_DOWNVOTE"],
    ...
  ],
  "content": "+1"
}
```
</details>

2 / 2 : [ NIP-58 Badge Award Event(s)](https://github.com/nostr-protocol/nips/blob/master/58.md#badge-award-event) exist- with IdentifierTag- as follows:

<details><summary>Kind: 30009 - <a href="https://github.com/avlo/superconductor/blob/d2448b2e8dc071ee0a34642785d3384d2ca1a9d6/base/src/main/java/com/prosilion/superconductor/base/service/event/type/SuperconductorKindType.java#L13">Badge Award UNIT_UPVOTE</a></summary>

```java
{
  ...
  "kind": 8,
  "pubkey": "<AUTHOR_PUBKEY>",
  "tags": [
    ["a", "30009:<BADGE_DEFINITION_SOURCE_RELAY-PUBKEY>:UNIT_UPVOTE"],
    ["p", "<VOTE_RECIPIENT_PUBKEY>"]
  ]
  ...
}     
```
</details>

<details><summary>Kind: 30009 - <a href="https://github.com/avlo/superconductor/blob/d2448b2e8dc071ee0a34642785d3384d2ca1a9d6/base/src/main/java/com/prosilion/superconductor/base/service/event/type/SuperconductorKindType.java#L13">Badge Award UNIT_DOWNVOTE</a></summary>

```java
{
  ...
  "kind": 8,
  "pubkey": "<AUTHOR_PUBKEY>",
  "tags": [
    ["a", "30009:<BADGE_DEFINITION_SOURCE_RELAY-PUBKEY>:UNIT_DOWNVOTE"],
    ["p", "<VOTE_RECIPIENT_PUBKEY>"]
  ]
  ...
}     
```
</details>

###### _see [SuperConductor nostr-relay](https://github.com/avlo/superconductor) for a compatible relay / reference implementation._
###### _see [AfterimageReqThenSuperconductorEventIT](src/test/java/com/prosilion/afterimage/service/reactive/AfterimageReqThenSuperconductorEventIT.java) for  integration test examples containing implementation details / code samples._

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

*user: h2dbuser  
*password: h2dbuserpass

Display all framework table contents (case-sensitive quoted fields/tables when querying):

    --select id, pub_key, session_id, challenge from auth;
    select id, event_id_string, kind, created_at, pub_key, content, concat(left(signature,20), '...') as signature from event;
    select id, event_id, event_tag_id from "event-event_tag-join";
    select id, event_id_string, recommended_relay_url, marker from event_tag;
    select id, event_id, pubkey_id from "event-pubkey_tag-join";
    select id, public_key, main_relay_url, pet_name from pubkey_tag;
    select id, event_id, identifier_tag_id from "event-identifier_tag-join";
    select id, uuid from identifier_tag;
    select id, event_id, address_tag_id from "event-address_tag-join";
    select id, kind, pub_key, uuid, relay_uri, code from address_tag;
    select id, event_id, reference_tag_id from "event-reference_tag-join";
    select id, uri from reference_tag;
    select id, event_id, subject_tag_id from "event-subject_tag-join";
    select id, subject from subject_tag;
    select id, event_id, hash_tag_id from "event-hashtag_tag-join";
    select id, hashtag_tag from hashtag_tag;
    select id, event_id, geohash_tag_id from "event-geohash_tag-join";
    select id, location from geohash_tag;
    select id, event_id, generic_tag_id  FROM "event-generic_tag-join";
    select id, code from generic_tag;
    select id, generic_tag_id, element_attribute_id from "generic_tag-element_attribute-join";
    select id, name, "value" from element_attribute;
    select id, event_id, price_tag_id from "event-price_tag-join";
    select id, number, currency, frequency from price_tag;
    select id, event_id from deletion_event;

##### (Optional Use) bundled web-client URLs for convenience/dev-testing/etc

http://localhost:5556/api-tests.html <sup>_(nostr **events** web-client)_</sup>

http://localhost:5556/request-test.html <sup>_(nostr **request** web-client)_</sup>
