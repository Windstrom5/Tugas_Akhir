package com.windstrom5.tugasakhir.connection

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable
interface LocationWebSocketService {
    @Receive
    fun observeLocationUpdates(): Flowable<LocationUpdate>

    @Send
    fun sendLocationUpdate(locationUpdate: LocationUpdate)
}