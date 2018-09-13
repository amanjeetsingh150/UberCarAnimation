package com.developers.uberanimation.utils;


import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**{@code Singleton class which is a Event Bus to receive the Journey related events for the user}
 * Currently 3 events are there: The begin of the journey, The end and the event to receive
 * current location event for driver.
 *
 * Created by Amanjeet Singh on 9/9/18.
 */
public class JourneyEventBus {


    private static final JourneyEventBus journeyEventBus = new JourneyEventBus();

    private PublishSubject<Object> subject = PublishSubject.create();

    private JourneyEventBus() {
    } //private constructor.

    public static synchronized JourneyEventBus getInstance() {
        return journeyEventBus;
    }

    public void setOnJourneyBegin(Object journeyEvent) {
        subject.onNext(journeyEvent);
    }

    public Observable<Object> getOnJourneyEvent() {
        return subject;
    }

    public void setOnJourneyEnd(Object journeyEvent) {
        subject.onNext(journeyEvent);
    }

    public void setOnJourneyUpdate(Object journeyEvent) {
        subject.onNext(journeyEvent);
    }
}
