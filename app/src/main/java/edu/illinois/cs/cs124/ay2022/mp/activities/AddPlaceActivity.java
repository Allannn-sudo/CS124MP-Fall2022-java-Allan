package edu.illinois.cs.cs124.ay2022.mp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import edu.illinois.cs.cs124.ay2022.mp.R;
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow;
import edu.illinois.cs.cs124.ay2022.mp.network.Client;
import java.util.function.Consumer;

public class AddPlaceActivity extends AppCompatActivity {
  private static final String TAG = AddPlaceActivity.class.getSimpleName();

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_addplace);

    Intent returnToMain = new Intent(this, MainActivity.class);
    returnToMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

    Button cancelButton = findViewById(R.id.cancel_button);
    cancelButton.setOnClickListener(
        v -> {
          startActivity(returnToMain);
        });

    Intent savePlace = new Intent(this, MainActivity.class);
    Intent retrieve = this.getIntent();
    Double latitude = Double.parseDouble(retrieve.getStringExtra("latitude"));
    Double longitude = Double.parseDouble(retrieve.getStringExtra("longitude"));
    String id = FavoritePlacesApplication.CLIENT_ID;
    EditText info = findViewById(R.id.description);
    savePlace.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

    Button saveButton = findViewById(R.id.save_button);
    saveButton.setOnClickListener(
        v -> {
          String description = info.getText().toString();
          String npg = "zz";
          Place favorite = new Place(id, "Shakesphere", latitude, longitude, npg, description);
          startActivity(savePlace);
          savePlace.putExtra("condition", 5);
          int condition = savePlace.getIntExtra("condition", 0);
          Consumer<ResultMightThrow<Boolean>> callback = null;
          if (condition == 5) {
            Client.start().postFavoritePlace(favorite, callback);
          }
        });
  }
}
