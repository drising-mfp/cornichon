package com.github.agourlay.cornichon.steps.wrapped

import java.util.concurrent.ScheduledExecutorService

import com.github.agourlay.cornichon.core._

import scala.concurrent.ExecutionContext

// Transparent Attach has no title - steps are flatten in the main execution
case class AttachStep(title: String = "", nested: List[Step]) extends WrapperStep {

  // remove AttachStep from remainingStep and prepend nested to remaing steps
  override def run(engine: Engine)(initialRunState: RunState)(implicit ec: ExecutionContext, timer: ScheduledExecutorService) =
    engine.runSteps(initialRunState.consumCurrentStep.prependSteps(nested))

}
