package br.com.youberrider.utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

import br.com.youberrider.model.Token;

public class UserUtils {
    public static FirebaseAuth auth = FirebaseAuth.getInstance();

    public static void updateUser(View view, Map<String, Object> updateData) {
        FirebaseDatabase.getInstance().getReference(Common.RIDER_INFO_REFERENCE)
                .child(auth.getCurrentUser().getUid())
                .updateChildren(updateData)
                .addOnSuccessListener(aVoid ->
                        Snackbar.make(view, "Upload realizado com sucesso!", Snackbar.LENGTH_SHORT).
                                setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).show()).
                addOnFailureListener(e -> Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_SHORT).
                        setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).show());

    }

    public static void updateToken(Context context, String token) {
        Token tokenModel = new Token(token);
        FirebaseDatabase.getInstance().getReference(Common.TOKEN_REFERENCE)
                .child(auth.getCurrentUser().getUid())
                .setValue(tokenModel)
                .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(aVoid -> {

                });
    }
}
