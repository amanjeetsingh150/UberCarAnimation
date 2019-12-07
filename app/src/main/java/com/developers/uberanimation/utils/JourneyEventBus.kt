package com.developers.uberanimation.utils


import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Singleton class which is a Event Bus to receive the Journey related events for the user.
 * Currently 3 events are there:
 * 1. The begin of the journey
 * 2. The end and the event to receive
 * 3. Current location event for driver.
 *
 * Created by Amanjeet Singh on 9/9/18.
 */
class JourneyEventBus private constructor() { //private constructor.
  private val subject = PublishSubject.create<Any>()

  val onJourneyEvent: Observable<Any>
    get() = subject

  fun setOnJourneyBegin(journeyEvent: Any) {
    subject.onNext(journeyEvent)
  }

  fun setOnJourneyEnd(journeyEvent: Any) {
    subject.onNext(journeyEvent)
  }

  fun setOnJourneyUpdate(journeyEvent: Any) {
    subject.onNext(journeyEvent)
  }

  companion object {
    @get:Synchronized
    val instance = JourneyEventBus()
  }
}
