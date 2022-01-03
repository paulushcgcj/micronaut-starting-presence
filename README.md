# Simple Presence Service

Imagine a simple service to manage user presence in a chat/group. The idea is that the complete system will authenticate the
user in another microservice, and that microservice will set some user information (such as session ID, name, roles, etc.).

After the authentication, a user can join a party/group chat to talk with their friends, and when it joins it, the party microservice
will handle the party association, by setting a key on redis with the existing members of a party. With this information at hand, 
the presence service (this service) will be able to return the status of a specific user (to be shown at its own app or someone else friends list),
 set a new status (when the user manually set it or due to inactivity) and also list the presence status of a specific party/group chat 
(reason why I mentioned the party microservice at the top).

## What is a presence?

A presence is basically the online presence or status of a user in a system. It will control if a user is active or not, and if a user has notified the whole 
system to not be disturbed, to be away or disconnection. 

## Why Redis?

I've chosen redis due to the stupidly fast write and read times (around 110k writes/second and 81k reads/second), 
making it a good choice to keep some temporary user data at hand.

## Shared Data

Ok, but what you have described is a shared database scenario (where multiple services will share the same database and/or data). Isn't this a bad approach?
Well, yes and no. Yes if you use redis as its final database (something we are not doing), and this will create a tight coupling between services 
and can cause some data integrity issues as well. In our case, redis is being used as a temporary, session/burnable only information, where everything is ephemeral, 
all data can and will be erased in a matter of time, and also, we deal with redis as it was a piece of our service infrastructure (such as a microservice that owns this temporary data).

It still can be interpreted as a bad approach? Yes it can. But it solves a few problems when handling this. Also, you don't have to share the same redis instance between the aforementioned services, 
each one can use its own redis cluster or db to handle this info, or you can cram all redis access to a single microservice, but this will turn a microservice into just a remote database driver instead.

This solution will solve one specific problem (handling presence) by consuming a small fraction of the data present on redis (we own only the presence data, the rest is there for reference/read-only purpose).

## Before Running

Make sure to have the environment up by running the docker compose file before and enabling [keyspace notification](https://redis.io/topics/notifications) by running `config set notify-keyspace-events Kxe` on redis-cli.


## Project Operation Diagrams

I will try to illustrate how this service should operate and also where it fits in a hypothetical scenario where it interacts with another systems.

### Internal Sequence Diagrams

During login:

```sequence {theme="hand"}
title Presence Service

actor Client #blue
boundary gRPC Endpoint
control Service
database Redis #red

Client->gRPC Endpoint:User logs into platform

note over Client:When a user logs in, **onOnline** is triggered automatically

gRPC Endpoint->Service:Trigger **updateUserStatus**

note over Service:Status is set as **ONLINE** when user logs in.


Service->Redis:Update status as **ONLINE**

note over Redis:The status will be added to the existing session hash entry

Service->Redis:Set expiration time to **2 min**

note over Redis:Keep in mind that this expiration time should be aligned with the **global** expiration time


gRPC Endpoint<-Service:Notify caller with current status

Client<-gRPC Endpoint:Notify user with current status
```