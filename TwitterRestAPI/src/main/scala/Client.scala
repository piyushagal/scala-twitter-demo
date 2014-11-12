package main.scala

import akka.actor.ActorSystem
import spray.http.BasicHttpCredentials
import spray.client.pipelining._
import akka.actor.Actor
import akka.actor.Cancellable
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import org.apache.commons.httpclient.util.URIUtil
import scala.util.Random
import spray.http.HttpRequest
import java.io.RandomAccessFile
import java.util.Date
import java.io.File
import akka.actor.Props
import spray.http.HttpResponse

object Client extends App {
  case class Start()
  case class Stop()
  implicit val system = ActorSystem()
  import system.dispatcher

  val pipeline = sendReceive

  val max = 1000
  for (i <- 1 to max) {
    val actor = system.actorOf(Props(new User(i, max)), "name" + i)
  }
  for (i <- 1 to max) {
    var actor = system.actorSelection("/user/name" + i)
    actor ! Start
  }
  Thread.sleep(60000L)
  for (i <- 1 to max) {
    var actor = system.actorSelection("/user/name" + i)
    actor ! Stop
  }

  pipeline(Get("http://localhost:2552/stop"))
  //Thread.sleep(1000L)
  //system.shutdown

  class User(id: Int, N: Int) extends Actor {
    val CREATE = "http://localhost:2552/create?userID="
    val FOLLOW = "http://localhost:2552/follow?userID="
    val TWEET = "http://localhost:2552/tweet?userID="
    val GETTWEETS = "http://localhost:2552/tweets?userID="
    implicit val system = context.system
    import system.dispatcher
    var userID = "name" + id
    var addFollower: Cancellable = null
    var postMessage: Cancellable = null
    var getTweets: Cancellable = null

    var noOfFollowers = 1 + new Random().nextInt(N)
    var count = 0

    val pipeline = sendReceive
    pipeline(Post(CREATE + userID))
    def receive = {
      case Start =>
        addFollower = context.system.scheduler.schedule(Duration.create(600, TimeUnit.MILLISECONDS),
          Duration.create(2000, TimeUnit.MILLISECONDS))(addFollowerFunc)
        postMessage = context.system.scheduler.schedule(Duration.create(1000, TimeUnit.MILLISECONDS),
          Duration.create(1000, TimeUnit.MILLISECONDS))(postMessageFunc)
        getTweets = context.system.scheduler.schedule(Duration.create(5000, TimeUnit.MILLISECONDS),
          Duration.create(5000, TimeUnit.MILLISECONDS))(getTweetsFunc)
      case Stop =>
        addFollower.cancel
        postMessage.cancel
        getTweets.cancel
        system.stop(self)
    }

    def addFollowerFunc() {
      if (count < noOfFollowers) {
        count += 1
        var follow = "name" + (1 + new Random().nextInt(N))
        val result = pipeline(Post(URIUtil.encodeQuery(FOLLOW + userID + "&follow=" + follow).toString))
        //printResult(result)
      } else {
        addFollower.cancel
      }
    }

    def postMessageFunc() {
      var tweet = new Tweet()
      tweet.set(userID)
      val result = pipeline(Post(URIUtil.encodeQuery(TWEET + userID + "&text=" + tweet.text).toString))
      //printResult(result)
    }
    def getTweetsFunc() {
      var tweet = new Tweet()
      tweet.set(userID)
      val result = pipeline(Get(URIUtil.encodeQuery(GETTWEETS  + userID).toString))
      printResult(result)
    }

    def printResult(result: scala.concurrent.Future[spray.http.HttpResponse]) {
      result.foreach {
        response =>
          println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
      }
    }
  }

  class Tweet extends Serializable {
    var userID: String = null
    var tweetID: String = null
    var timeStamp: String = null
    var text: String = null

    def set(uid: String) {
      userID = uid
      timeStamp = (new Date().setTime(System.currentTimeMillis())).toString()
      tweetID = uid + System.currentTimeMillis()
      text = getFromFile()
    }

    def getFromFile(): String = {
      val file = new File("E:\\temp\\Twitter\\src\\sample.txt")
      val raf = new RandomAccessFile(file, "r")
      val start = new Random().nextInt(4000)
      raf.seek(start)
      var dst = new Array[Byte](140)
      raf.readFully(dst)
      var s = new String(dst)
      s
    }
  }

}