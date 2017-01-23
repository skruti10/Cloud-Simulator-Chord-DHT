package abhijay

import java.io.IOException
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import grizzled.slf4j.Logger

import scala.concurrent.duration._
import scala.io.Source
import scala.util.Random

/**
  * Created by avp on 11/24/2016.
  */

object MyUserActorDriver {

  val actorSystem = ActorSystem(ParameterConstants.userActorName);
  val logger = Logger("Simulation started");

  def main(args: Array[String]): Unit = {
    logger.info(args.deep.mkString(", "));
    if(args.length != 0){
      ParameterConstants.numberOfUsers = args(0).toInt;
      ParameterConstants.minRequests = args(1).toInt;
      ParameterConstants.maxRequests = args(2).toInt;
      ParameterConstants.ratio = args(3);
      ParameterConstants.interval = args(4).toInt;
    }

/*
      block to get max read and write requests from ratio, min and max requests
      e.g. ration = 4:1, minRequests = 0, maxRequests = 13
      divider = 13/(4+1) = 13/5 = 2
*/
      val tokens = ParameterConstants.ratio.split("\\:");
      val readRequests = tokens(0).toInt;
      val writeReqeusts = tokens(1).toInt;
      val totalRequests = readRequests + writeReqeusts;
      val divider = (ParameterConstants.maxRequests / totalRequests).toInt;
      val maxReadRequests = divider * readRequests;
      val maxWriteRequests = divider * writeReqeusts;

    logger.info(readRequests)
    logger.info(writeReqeusts)
    logger.info(totalRequests)
    logger.info(maxReadRequests)
    logger.info(maxWriteRequests)

//    loadTest(5000);

    // instantiate all the actors
    instantiateActors(ParameterConstants.numberOfUsers, actorSystem);
    startSimulation(ParameterConstants.duration, ParameterConstants.numberOfUsers, maxReadRequests, maxWriteRequests, divider);
    takeSnapshots(ParameterConstants.interval);


  }

  // We used this method to load test our cloud simulator. Input to the method is number of nodes in the system.
  def loadTest(maxNodes: Int): Unit ={
    for(i <- 0 until (maxNodes/10)){
      val random = Random;
      val id = random.nextInt(maxNodes);
      val url = "http://127.0.0.1:8080/?insertNode=" + id;
      println(url);
      getURLContent(url);
    }
  }

  def startSimulation(duration: Int, numberOfUsers: Int,
                      maxReadRequests: Int,
                      maxWriteRequests: Int,
                      maxDeleteRequests: Int): Unit ={
    val simulationDuration = duration.seconds.fromNow;
    val random = Random;
    for(i <- 0 until numberOfUsers){
      val userNode = actorSystem.actorSelection(ParameterConstants.userActorNamePrefix + ParameterConstants.userNamePrefix + i);
      logger.info("startSimulation() " + userNode.pathString);
      userNode ! writeRequest(0, maxWriteRequests);
      userNode ! readRequest(0, maxReadRequests);
      userNode ! deleteRequest(0, maxDeleteRequests);
      Thread.sleep(1000);
    }
  }

  def takeSnapshots(interval: Int): Unit = {
    import actorSystem.dispatcher
    val url = "http://127.0.0.1:8080/getSnapshot";
    actorSystem.scheduler.schedule(Duration(5, TimeUnit.SECONDS), Duration(interval*60, TimeUnit.SECONDS)) {
      getURLContent(url);
    }
  }

  // retrieve contents from URL
  def getURLContent(url: String) : String = {
    var result: String = "";
    try{
      result = scala.io.Source.fromURL(url).mkString;
    }
    catch{
      case ioe: IOException =>{
        logger.info("IOException in getURLContent. Ignoring.");
      }
      case _: Throwable => {
        logger.info("Exception in getURLContent. Ignoring.");
      }
    }
    return result;
  }

  // add all read movies in cloud simulator
  def addAllMovies(fileName: String): Unit ={

  }

  // read input file and return list of lines
  def readFile(fileName: String) : List[String] = {
    val listOfLines = Source.fromFile(fileName).getLines.toList
    return listOfLines;
  }

  // instantial all user actors
  def instantiateActors(numberOfActors: Int, actorSystem: ActorSystem): Unit = {
    for(i <- 0 until numberOfActors){
      var user = actorSystem.actorOf(Props(new UserActor(i)), name = ParameterConstants.userNamePrefix+i);
    }
  }
}
