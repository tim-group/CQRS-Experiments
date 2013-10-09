import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import java.util.Date

class Bus extends Actor {
  var subscribers: List[ActorRef] = Nil

  def receive = {
    case Subscribe(actor) => subscribers = actor :: subscribers
    case msg => subscribers.foreach(subscriber => subscriber ! msg)
  }
}

class CommandHandler(eventBus: ActorRef) extends Actor {
  def receive = {
    case CreateRecommendation(ric, direction) => {
      val event = RecommendationCreated(new Date(), ric, direction)

      eventBus ! event
    }
  }
}

class LiveSentimentEventHandler extends Actor {
  def receive = {
    case RecommendationCreated(_, _, direction) => {
      LiveSentimentProjection.reset(LiveSentimentProjection.current + direction)
    }
  }
}

object LiveSentimentProjection {
  private var currentSentiment: Int = 0

  def current = currentSentiment

  def reset(sentiment: Int) = {
    currentSentiment = sentiment

    println("The current sentiment is: " + currentSentiment)
  }
}

object App {
  val system = ActorSystem("CQRSActorSystem")

  val commandBus = system.actorOf(Props[Bus], name = "CommandBus")
  val eventBus = system.actorOf(Props[Bus], name = "EventBus")
  val commandHandler = system.actorOf(Props(classOf[CommandHandler], eventBus), name = "CommandHandler")
  val liveSentimentEventHandler = system.actorOf(Props[LiveSentimentEventHandler], name = "LiveSentimentEventHandler")

  commandBus ! Subscribe(commandHandler)
  eventBus ! Subscribe(liveSentimentEventHandler)

  println("The app was started..")

  def apply(command: Command) = {
    commandBus ! command
  }

  def stop() {
    system.shutdown()
  }
}

case class Subscribe(actor: ActorRef)

trait Command

trait Event {
  def timestamp: Date
}

case class CreateRecommendation(ric: String, direction: Int) extends Command

case class RecommendationCreated(timestamp: Date, ric: String, direction: Int) extends Event