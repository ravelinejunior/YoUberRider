package br.com.youberrider.services;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

import br.com.youberrider.utils.Common;
import br.com.youberrider.utils.UserUtils;

public class MyFirebaseServiceMessaging extends FirebaseMessagingService {

    private FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        if(auth.getCurrentUser() != null) UserUtils.updateToken(this,s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String,String> map = remoteMessage.getData();
        if(map != null){
            Common.showNotification(this,
                    new Random().nextInt(),
                      map.get(Common.NOTI_TITLE),
                         map.get(Common.NOTI_CONTENT),
                            null);
        }
    }
}
