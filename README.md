scala-twitter-demo
==================

This project simulate distributed client and server interactions of Twitter. Server handles Http requests from User.
This project is based on akka actors and spray-can. 
Major understandings created by the project are :
1) How multiple actors can handle requests from millions of users efficiently?
2) How a single RestAPI interface can handle requests from millions of users and work as router to 
transfer heavy work to background actors.
3) Used stats of twitter to simulate real world scenarios of request tweets, tweet and follow.
