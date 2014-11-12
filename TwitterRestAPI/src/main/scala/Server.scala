package main.scala

import spray.routing.SimpleRoutingApp
import akka.actor.ActorSystem
import java.util.HashMap
import scala.collection.immutable.List
import akka.actor.Actor
import akka.actor.Props
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import org.json4s.ShortTypeHints
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import spray.http.HttpEntity
import spray.can.Http
import spray.can.server.Stats
import spray.http.MediaTypes._
import java.io.FileWriter
import java.awt.Desktop
import java.io.File
import scala.util.Random

object Server extends App with SimpleRoutingApp {
  implicit val actorSystem = ActorSystem()
  import actorSystem.dispatcher
  implicit val timeout = Timeout(1.seconds)
  class stats {
    var noOfUsers = 0
    var noOfFollowers = 0
    var noOfTweets = 0
    var noOfGetTweets = 0
  }
  var s = new stats
  var followerMap = new HashMap[String, UserFollowers]
  var tweetMap = new HashMap[String, UserHomeTweets]
  val tweetUpdate = actorSystem.actorOf(Props(new TweetUpdate()))
  val followerUpdate = actorSystem.actorOf(Props(new FollowerUpdate()))
  val homepagetweets = actorSystem.actorOf(Props(new HomePageTweets()))
  val updatestats = actorSystem.actorOf(Props(new UpdateStats()))

  startServer(interface = "localhost", port = 2552) {
    post {
      path("create") {
        parameter("userID") { userID =>
          s.noOfUsers += 1
          complete {
            "Created!"
          }
        }
      }
    } ~
      post {
        path("follow") {
          parameter("userID", "follow") { (userID, follow) =>
            followerUpdate ! (userID, follow)
            s.noOfFollowers += 1
            complete {
              "Follower Added ! "
            }
          }
        }
      } ~
      post {
        path("tweet") {
          parameter("userID", "text") { (userID, text) =>
            tweetUpdate ! (userID, text)
            s.noOfTweets += 1
            updatestats ! (s)
            complete {
              "Tweet Added ! "
            }
          }
        }
      } ~
      get {
        path("stop") {
          complete {
            "Server Stopped!!"
          }

          ctx => println("No Of Users : " + s.noOfUsers)
          println("No Of Tweets : " + s.noOfTweets)
          println("No Of Followers/user  : " + s.noOfFollowers.toDouble / s.noOfUsers.toDouble)
          Thread.sleep(1000L)
          actorSystem.shutdown
        }
      } ~
      get {
        path("tweets") {
          parameter("userID") { userID =>
            s.noOfGetTweets += 1
            complete {
              (homepagetweets ? userID)
                .mapTo[String]
                .map(s => s"The tweets are : $s")
            }
          }
        }
      } ~
      get {
        path("stats") {
          val entity = HttpEntity(`text/html`,
            <html>
              <body>
                <h1>HttpServer Stats</h1>
                <table>
                  <tr><td>Number of Users:</td><td>{ s.noOfUsers }</td></tr>
                  <tr><td>Number of Tweets:</td><td>{ s.noOfTweets }</td></tr>
                </table>
              </body>
            </html>.toString())
          complete {
            entity
          }
        }
      }
  }
  class TweetUpdate extends Actor {
    def receive = {
      case (userID: String, text: String) =>
        var tweetList = tweetMap.get(userID)
        if (tweetList == null) {
          tweetList = new UserHomeTweets()
          tweetList.userID = userID
        }
        tweetList.tweets = "Me" + " : " + text :: tweetList.tweets
        tweetMap.put(userID, tweetList)
        var list = followerMap.get(userID)
        if (list != null) {
          for (x <- list.followerSet) {
            var followertweetList = tweetMap.get(x)
            if (followertweetList == null) {
              followertweetList = new UserHomeTweets()
              followertweetList.userID = x
            }
            followertweetList.tweets = userID + " : " + text :: followertweetList.tweets
            tweetMap.put(x, followertweetList)
          }
        }
    }
  }
  class FollowerUpdate extends Actor {
    def receive = {
      case (userID: String, follow: String) =>
        var list = followerMap.get(follow)
        if (list == null) {
          list = new UserFollowers()
          list.userID = follow
        }
        list.followerSet += userID
        followerMap.put(follow, list)
    }
  }
  class HomePageTweets extends Actor {
    private implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[String])))
    def receive = {
      case (userID: String) =>
        sender ! writePretty(tweetMap.get(userID).tweets)
    }
  }
  class UpdateStats extends Actor {
    var p = "E:\\stats.html"
    def writeToFile(s: String): Unit = {
      val pw = new java.io.PrintWriter(new FileWriter(p, false))
      try pw.write(s + "\n") finally pw.close()
    }
    writeToFile(
      <html>
        <meta http-equiv="refresh" content="5"/>
        <body>
          <h1>HttpServer Stats</h1>
          <table>
            <tr><td>Number of Users:</td><td>{ 0 }</td></tr>
            <tr><td>Number of Tweets:</td><td>{ 0 }</td></tr>
            <tr><td>Average Followers:</td><td>{ 0 }</td></tr>
          </table>
        </body>
      </html>.toString())
    var htmlFilePath = "E:\\stats.html"; // path to your new file
    var htmlFile = new File(htmlFilePath);

    // open the default web browser for the HTML page
    Desktop.getDesktop().browse(htmlFile.toURI());

    def receive = {
      case (s: stats) =>
        var write = new Random().nextInt(10) % 10 == 5
        if (write) {
          writeToFile(
            <html>
              <meta http-equiv="refresh" content="5.5"/>
              <body>
                <h1>HttpServer Stats</h1>
                <table>
                  <tr><td>Number of Users:</td><td>{ s.noOfUsers }</td></tr>
                  <tr><td>Average Followers:</td><td>{ s.noOfFollowers / s.noOfUsers }</td></tr>
                  <tr><td>Number of Tweets:</td><td>{ s.noOfTweets }</td></tr>
                  <tr><td>Number of Get Tweets Request:</td><td>{ s.noOfGetTweets }</td></tr>
                </table>
              </body>
            </html>.toString())
        }

    }
  }

}

class UserHomeTweets {
  var userID: String = "";
  var tweets = List[String]()
}

class UserFollowers {
  var userID: String = "";
  var followerSet = Set.empty[String]
}