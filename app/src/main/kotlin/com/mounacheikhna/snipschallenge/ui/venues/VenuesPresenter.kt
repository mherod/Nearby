package com.mounacheikhna.snipschallenge.ui.venues

import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.mounacheikhna.snipschallenge.api.FoursquareApi
import com.mounacheikhna.snipschallenge.ui.VenueResult
import com.mounacheikhna.snipschallenge.ui.base.BasePresenter
import pl.charmas.android.reactivelocation.ReactiveLocationProvider
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.lang.kotlin.BehaviourSubject
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Presenter for {@link VenuesView}
 */
//@ScopeSingleton(VenuesView.VenuesComponent::class)
@Singleton //temp
class VenuesPresenter : BasePresenter<VenuesScreen> {

    lateinit var searchVenuesSubject: Observable<VenueResult>
    val locationProvider: ReactiveLocationProvider
    val foursquareApi: FoursquareApi

    @Inject
    public constructor(foursquareApi: FoursquareApi, locationProvider: ReactiveLocationProvider) : super() {
        this.foursquareApi = foursquareApi
        this.locationProvider = locationProvider
    }

    /**
     * Fetch nearby venues each time a location change but gives the user a message saying it will
     * fetch so that if they don't want to they can cancel it.
     */
    fun fetchVenuesForLocations(): Observable<VenueResult> {
        val updatedLocation = locationProvider.getUpdatedLocation(
            createLocationRequest()).distinctUntilChanged()
        var locationSubscription = updatedLocation
            .subscribe { location ->
                view?.onNewLocationUpdate()
            }
        addSubscription(locationSubscription)
        return updatedLocation.flatMap { it -> startNearbyVenuesSearch(it) }
                      //.takeUntil(view?.cancelRefreshForLocation())

        //return searchVenuesSubject
    }

    /**
     * Fetches venues near a {@link Location} then for each id fetches the {@link VenueResult}
     * to get venue's details, ratings and photos then displays them.
     *
     * @param location to fetch venues near it.
     * @return subscription to unsubscribe from it.
     */
    fun startNearbyVenuesSearch(location: Location): Observable<VenueResult> {
        //var searchSubject = BehaviourSubject<VenueResult>()
        val searchObservable = foursquareApi.searchVenues(
            "${location.latitude}, ${location.longitude}")
            .flatMapIterable { it -> it.response.venues }
        val venueDetailsObservable = searchObservable.flatMap { it ->
            foursquareApi.venueDetails(it.id)
        }
        val venuePhotosObservable = searchObservable.flatMap { it ->
            foursquareApi.venuePhotos(it.id, 1)
        }

        return Observable.zip(venueDetailsObservable, venuePhotosObservable,
            { respVenues, respPhotos ->
                VenueResult(respVenues.response.venue, respPhotos.getMainPhotoUrl())
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            //.subscribe(searchSubject)

        /*addSubscription(subscription)
        return searchSubject*/
    }

    /**
     * Creates a {@link LocationRequest} which defines the rate of location updates : accuracy,
     * update interval and number of updates.
     *
     * @return locationRequest the request to define updates rate.
     */
    fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            .setInterval(1000000)
        return locationRequest
    }

}