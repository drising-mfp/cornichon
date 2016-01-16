package com.github.agourlay.cornichon.core

import org.scalatest.{ Matchers, WordSpec }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class EngineSpec extends WordSpec with Matchers {

  val engine = new Engine(ExecutionContext.global)

  "An engine" when {
    "runScenario" must {
      "execute all steps of a scenario" in {
        val session = Session.newSession
        val steps = Vector(RunnableStep[Int]("first step", s ⇒ (s, SimpleStepAssertion(2 + 1, 3))))
        val s = Scenario("test", steps)
        engine.runScenario(session)(s).isInstanceOf[SuccessScenarioReport] should be(true)
      }

      "fail if instruction throws exception" in {
        val session = Session.newSession
        val steps = Vector(
          RunnableStep[Int]("stupid step", s ⇒ {
            6 / 0
            (s, SimpleStepAssertion(2, 2))
          })
        )
        val s = Scenario("scenario with stupid test", steps)
        engine.runScenario(session)(s).isInstanceOf[FailedScenarioReport] should be(true)
      }

      "stop at first failed step" in {
        val session = Session.newSession
        val step1 = RunnableStep[Int]("first step", s ⇒ (s, SimpleStepAssertion(2, 2)))
        val step2 = RunnableStep[Int]("second step", s ⇒ (s, SimpleStepAssertion(4, 5)))
        val step3 = RunnableStep[Int]("third step", s ⇒ (s, SimpleStepAssertion(1, 1)))
        val steps = Vector(
          step1, step2, step3
        )
        val s = Scenario("test", steps)
        engine.runScenario(session)(s) match {
          case s: SuccessScenarioReport ⇒ fail("Should be a FailedScenarioReport")
          case f: FailedScenarioReport ⇒
            f.failedStep.error.msg.replaceAll("\r", "") should be("""
            |expected result was:
            |'4'
            |but actual result is:
            |'5'""".stripMargin.trim)
            f.successSteps should be(Vector(step1.title))
            f.notExecutedStep should be(Vector(step3.title))
        }
      }

      "replay eventually wrapped steps" in {
        val session = Session.newSession
        val eventuallyConf = EventuallyConf(maxTime = 5.seconds, interval = 10.milliseconds)
        val steps = Vector(
          EventuallyStart(eventuallyConf),
          RunnableStep(
            "possible random value step", s ⇒ {
              (s, SimpleStepAssertion(scala.util.Random.nextInt(10), 5))
            }
          ),
          EventuallyStop(eventuallyConf)
        )
        val s = Scenario("scenario with eventually", steps)
        engine.runScenario(session)(s).isInstanceOf[SuccessScenarioReport] should be(true)
      }

      "replay eventually wrapped steps until limit" in {
        val session = Session.newSession
        val eventuallyConf = EventuallyConf(maxTime = 10.milliseconds, interval = 1.milliseconds)
        val steps = Vector(
          EventuallyStart(eventuallyConf),
          RunnableStep(
            "impossible random value step", s ⇒ {
              (
                s,
                SimpleStepAssertion(11, scala.util.Random.nextInt(10))
              )
            }
          ),
          EventuallyStop(eventuallyConf)
        )
        val s = Scenario("scenario with eventually that fails", steps)
        engine.runScenario(session)(s).isInstanceOf[FailedScenarioReport] should be(true)
      }

      "success if non equality was expected" in {
        val session = Session.newSession
        val steps = Vector(
          RunnableStep(
            "non equals step", s ⇒ {
              (s, SimpleStepAssertion(1, 2))
            }, negate = true
          )
        )
        val s = Scenario("scenario with unresolved", steps)
        engine.runScenario(session)(s).isInstanceOf[SuccessScenarioReport] should be(true)
      }

      "return error if a Debug step throw an exception" in {
        val session = Session.newSession
        val step = DebugStep(s ⇒ {
          6 / 0
          "Never gonna read this"
        })
        val s = Scenario("scenario with faulty debug step", Vector(step))
        engine.runScenario(session)(s).isInstanceOf[FailedScenarioReport] should be(true)
      }
    }

    "runStepAction" must {
      "return error if Executable step throw an exception" in {
        val session = Session.newSession
        val step = RunnableStep[Int]("stupid step", s ⇒ {
          6 / 0
          (s, SimpleStepAssertion(2, 2))
        })
        engine.runStepAction(step)(session).isLeft should be(true)
      }
    }

    "runStepPredicate" must {
      "return session if success" in {
        val session = Session.newSession
        val step = RunnableStep[Int]("stupid step", s ⇒ {
          6 / 0
          (s, SimpleStepAssertion(2, 2))
        })
        engine.runStepPredicate(negateStep = false, session, StepAssertion.alwaysOK).fold(e ⇒ fail("should have been Right"), s ⇒ s should be(session))
      }
    }

    "findEnclosedStep" must {
      "resolve non nested sub steps" in {
        val eventuallyConf = EventuallyConf(maxTime = 10.milliseconds, interval = 1.milliseconds)
        val steps = Vector(
          EventuallyStart(eventuallyConf),
          RunnableStep[Int]("first step", s ⇒ (s, SimpleStepAssertion(2 + 1, 3))),
          RunnableStep[Int]("second step", s ⇒ (s, SimpleStepAssertion(2 + 1, 3))),
          EventuallyStop(eventuallyConf)
        )
        engine.findEnclosedSteps(steps.head, steps.tail).size should be(2)
      }

      "resolve non nested aligned sub steps" in {
        val eventuallyConf = EventuallyConf(maxTime = 10.milliseconds, interval = 1.milliseconds)
        val steps = Vector(
          EventuallyStart(eventuallyConf),
          RunnableStep[Int]("first step", s ⇒ (s, SimpleStepAssertion(2 + 1, 3))),
          RunnableStep[Int]("second step", s ⇒ (s, SimpleStepAssertion(2 + 1, 3))),
          EventuallyStop(eventuallyConf),
          EventuallyStart(eventuallyConf),
          RunnableStep[Int]("third step", s ⇒ (s, SimpleStepAssertion(2 + 1, 3))),
          EventuallyStop(eventuallyConf)
        )
        engine.findEnclosedSteps(steps.head, steps.tail).size should be(2)
      }

      "resolve nested sub steps" in {
        val eventuallyConf = EventuallyConf(maxTime = 10.milliseconds, interval = 1.milliseconds)
        val steps = Vector(
          EventuallyStart(eventuallyConf),
          RunnableStep[Int]("first step", s ⇒ (s, SimpleStepAssertion(2 + 1, 3))),
          RunnableStep[Int]("second step", s ⇒ (s, SimpleStepAssertion(2 + 1, 3))),
          EventuallyStart(eventuallyConf),
          RunnableStep[Int]("third step", s ⇒ (s, SimpleStepAssertion(2 + 1, 3))),
          EventuallyStop(eventuallyConf),
          EventuallyStop(eventuallyConf)
        )
        engine.findEnclosedSteps(steps.head, steps.tail).size should be(5)
      }
    }
  }
}
