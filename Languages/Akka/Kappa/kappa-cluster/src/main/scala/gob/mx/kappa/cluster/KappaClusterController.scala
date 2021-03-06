package gob.mx.kappa.cluster

import akka.actor.Actor
import akka.actor.Props
import akka.actor.Terminated
import akka.actor.SupervisorStrategy
import akka.actor.ActorLogging
import akka.actor.ReceiveTimeout
import scala.concurrent.duration._
import akka.actor.OneForOneStrategy

object KappaClusterController {
  case class Check(url: String, depth: Int)
  case class Result(links: Set[String])
}

class KappaClusterController extends Actor with ActorLogging {
  import KappaClusterController._

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5) {
    case _: Exception => SupervisorStrategy.Restart
  }

  var cache = Set.empty[String]

  context.setReceiveTimeout(10.seconds)

  def getterProps(url: String, depth: Int): Props = Props(new KappaTaskGetter(url, depth))

  def receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)
      if (!cache(url) && depth > 0)
        context.watch(context.actorOf(getterProps(url, depth - 1)))
      cache += url
    case Terminated(_) =>
      if (context.children.isEmpty)
        context.parent ! Result(cache)
    case ReceiveTimeout =>
      context.children foreach context.stop
  }

}