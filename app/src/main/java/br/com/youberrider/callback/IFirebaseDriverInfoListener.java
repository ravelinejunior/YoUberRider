package br.com.youberrider.callback;

import br.com.youberrider.model.DriverGeo;

public interface IFirebaseDriverInfoListener {
    void onDriverInforLoadSuccess(DriverGeo driverGeo);
}
