package edu.illinois.cs.cs124.ay2022.mp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import edu.illinois.cs.cs124.ay2022.mp.R;
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

/*
 * App main activity.
 * Started when the app is launched, based on the configuration in the Android Manifest
 * (AndroidManifest.xml).
 * Should display places on the map based on data retrieved from the server.
 *
 * You will need to understand some of the code here and make changes to complete most project
 * checkpoints.
 */
@SuppressWarnings("FieldCanBeLocal")
public final class MainActivity extends AppCompatActivity
    implements Consumer<ResultMightThrow<List<Place>>>,
        SearchView.OnQueryTextListener,
        MapEventsReceiver {
  // You may find this useful when adding logging
  private static final String TAG = MainActivity.class.getSimpleName();

  // Reference to the MapView, initialized in onCreate, handy to have in other places
  private MapView mapView = null;

  // Reference to Application instance, initialized in onCreate, handy to have in other places
  private FavoritePlacesApplication favoritePlacesApplication = null;

  // List of all places retrieved from the server, initially set to an empty list to avoid nulls
  private List<Place> allPlaces = new ArrayList<>();

  // ID of the currently open place, used to keep the same popup open when the list of places is
  // updated
  // null indicates no currently open popup
  private String openPlace = null;

  // Map boundaries, used to limit the scrollable area.
  // Our tile server does not provide tiles outside this geographic region.
  public static final double MAP_LIMIT_NORTH = 40.1741;
  public static final double MAP_LIMIT_SOUTH = 40.0247;
  public static final double MAP_LIMIT_WEST = -88.3331;
  public static final double MAP_LIMIT_EAST = -88.1433;

  // Max and default map zoom levels
  public static final double MAP_MIN_ZOOM = 12.0;
  public static final double MAP_DEFAULT_ZOOM = 17.0;
  /*
   * onCreate is the first method called when this activity is created.
   * Code here normally does a variety of setup tasks, and functions somewhat similarly to a
   * constructor.
   */
  private ImageView imageView;

  @Override
  protected void onCreate(final Bundle unused) {
    super.onCreate(unused);

    // Store a reference to the application instance so that we can access it in other methods
    favoritePlacesApplication = (FavoritePlacesApplication) getApplication();

    // Load the layout for this activity and set the title
    setContentView(R.layout.activity_main);
    setTitle("Favorite Places");

    // Find the MapView component in the layout and configure it properly
    // Also save the reference for later use
    mapView = findViewById(R.id.map);

    SearchView searchView = findViewById(R.id.search);
    searchView.setOnQueryTextListener(this);

    // A OpenStreetMaps tile source provides the tiles that are used to render the map.
    // We use our own tile source with relatively-recent tiles for the Champaign-Urbana area, to
    // avoid adding load to existing OSM tile servers.
    mapView.setTileSource(
        new XYTileSource(
            "CS124", 12, 18, 256, ".png", new String[] {"https://tiles.cs124.org/tiles/"}));

    // Limit the map to the Champaign-Urbana area, which is also the only area that our tile server
    // can provide tiles for.
    mapView.setScrollableAreaLimitLatitude(MAP_LIMIT_NORTH, MAP_LIMIT_SOUTH, 0);
    mapView.setScrollableAreaLimitLongitude(MAP_LIMIT_WEST, MAP_LIMIT_EAST, 0);

    // Only allow zooming out so far
    mapView.setMinZoomLevel(MAP_MIN_ZOOM);

    // Set the current map zoom level to the default
    IMapController mapController = mapView.getController();
    mapController.setZoom(MAP_DEFAULT_ZOOM);
    mapController.setCenter(new GeoPoint(40.10986682167534, -88.22831928981661));
    imageView = findViewById(R.id.imageView);
  }

  /*
   * onResume is called right before the activity begins interacting with the user.
   * So this is a good time to update our list of places.
   * We pass the MainActivity as the callback to the call to getPlaces, which is why this class
   * implements Consumer<ResultMightThrow<List<Place>>>, a functional interface allowing
   * our networking client to pass back the list of places to us once the network call completes.
   * We'll discuss this more when we talk about networking in Android on MP2.
   */
  @Override
  protected void onResume() {
    super.onResume();
    Log.i(TAG, "Entering onResume");
    favoritePlacesApplication.getClient().getPlaces(this);
  }

  /*
   * Called by code in Client.java when the call to retrieve the list of places from the server
   * completes.
   * We save the full list of places and update the UI.
   * Note the use of the ResultMightThrow to have the exception thrown and caught here.
   * This is due to how Android networking requests are handled.
   * For a longer explanation, see the note on ResultMightThrow.java.
   */
  @Override
  public void accept(final ResultMightThrow<List<Place>> result) {
    Log.i(TAG, "List of places is available");
    // We use a try-catch because getResult throws if the result contains an exception
    try {
      // Save the list of all available places
      allPlaces = result.getResult();
      // Update the UI to show all available places
      updateShownPlaces(allPlaces);
    } catch (Exception e) {
      e.printStackTrace();
      Log.e(TAG, "getPlaces threw an exception: " + result.getException());
    }
  }

  /*
   * Update the list of places shown on the map.
   *
   * Helper method used to convert our List<Place> to a set of markers that will appear on the map
   * drawn by osmdroid.
   */
  private void updateShownPlaces(final List<Place> showPlaces) {
    /*
     * Go through all existing overlays that are markers and close their popups.
     * If we don't do this, updates to the list of places that are currently visible can leave
     * open popups that aren't connected to any marker.
     * This seems like a bug in osmdroid,
     * reported here: https://github.com/osmdroid/osmdroid/issues/1858.
     */
    for (int i = 0; i < mapView.getOverlays().size(); i++) {
      Overlay existing = mapView.getOverlays().get(i);
      if (!(existing instanceof Marker)) {
        continue;
      }
      Marker marker = (Marker) existing;
      marker.closeInfoWindow();
    }

    // Clear all overlays and the ID of the currently open info window
    mapView.getOverlays().clear();
    String newOpenPlace = null;

    // Create markers for each place in our list and add them to the map
    for (Place place : showPlaces) {
      // Create a new Marker
      Marker marker = new Marker(mapView);

      // Set the ID so that we can track which marker has an open popup
      marker.setId(place.getId());

      // Set the position and other attributes appropriately
      marker.setPosition(new GeoPoint(place.getLatitude(), place.getLongitude()));
      marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
      marker.setTitle(place.getDescription());

      /*
       * Normally clicking on the marker both opens the popup and recenters the map.
       * The map recentering is a bit annoying, so we override this callback here to disable it.
       * The argument to setOnMarkerClickListener is just a lambda function called whenever the
       * marker is clicked.
       * This also allows us to track which marker was open.
       */
      marker.setOnMarkerClickListener(
          (m, unused) -> {
            if (!m.isInfoWindowShown()) {
              m.showInfoWindow();
              openPlace = m.getId();
            } else {
              m.closeInfoWindow();
              openPlace = null;
            }
            return true;
          });

      // Preserve the currently open place if there was one, and reopen the popup on the
      // appropriate marker
      if (marker.getId().equals(openPlace)) {
        marker.showInfoWindow();
        newOpenPlace = openPlace;
      }

      // Add the marker to the map
      mapView.getOverlays().add(marker);
    }

    // Update the currently-open marker
    // This will clear openPlace if the marker that was previously shown is no longer open
    openPlace = newOpenPlace;

    mapView.getOverlays().add(new MapEventsOverlay(this));

    // Force the MapView to redraw so that we see the updated list of markers
    mapView.invalidate();
  }

  @Override
  public boolean onQueryTextChange(final String text) {
    Log.d(TAG, "onQueryTextChange " + text);
    if (Place.search(allPlaces, text).size() == 0) {
      updateShownPlaces(allPlaces);
    } else {
      updateShownPlaces(Place.search(allPlaces, text));
    }
    return true;
  }

  @Override
  public boolean onQueryTextSubmit(final String text) {
    return false;
  }

  @Override
  public boolean singleTapConfirmedHelper(final GeoPoint p) {
    if ((p.getLatitude() >= 40.0931 && p.getLatitude() <= 40.0932)
        && (p.getLongitude() >= -88.2173 && p.getLongitude() <= -88.2172)) {
      imageView.setImageResource(R.drawable.thomas_cai);
    } else if ((p.getLatitude() >= 40.1124 && p.getLatitude() <= 40.1125)
        && (p.getLongitude() >= -88.2283 && p.getLongitude() <= -88.2282)) {
      imageView.setImageResource(R.drawable.william_deng);
    } else if ((p.getLatitude() >= 40.1134 && p.getLatitude() <= 40.1135)
        && (p.getLongitude() >= -88.2374 && p.getLongitude() <= -88.2373)) {
      imageView.setImageResource(R.drawable.anna_liu);
    } else if ((p.getLatitude() >= 40.1094 && p.getLatitude() <= 40.1095)
        && (p.getLongitude() >= -88.2306 && p.getLongitude() <= -88.2305)) {
      imageView.setImageResource(R.drawable.akash_kumar);
    } else if ((p.getLatitude() >= 40.0807 && p.getLatitude() <= 40.0808)
        && (p.getLongitude() >= -88.2038 && p.getLongitude() <= -88.2037)) {
      imageView.setImageResource(R.drawable.chris_whamond);
    } else if ((p.getLatitude() >= 40.1176 && p.getLatitude() <= 40.1177)
        && (p.getLongitude() >= -88.2434 && p.getLongitude() <= -88.2433)) {
      imageView.setImageResource(R.drawable.anagha_shenoy);
    } else if ((p.getLatitude() >= 40.1165 && p.getLatitude() <= 40.1166)
        && (p.getLongitude() >= -88.2297 && p.getLongitude() <= -88.2296)) {
      imageView.setImageResource(R.drawable.haosen_yao);
    } else if ((p.getLatitude() >= 40.1166 && p.getLatitude() <= 40.1167)
        && (p.getLongitude() >= -88.2408 && p.getLongitude() <= -88.2407)) {
      imageView.setImageResource(R.drawable.ruisong_li);
    } else if ((p.getLatitude() >= 40.1094 && p.getLatitude() <= 40.1095)
        && (p.getLongitude() >= -88.2284 && p.getLongitude() <= -88.2283)) {
      imageView.setImageResource(R.drawable.gautham_jeyasankarane);
    } else if ((p.getLatitude() >= 40.1120 && p.getLatitude() <= 40.1121)
        && (p.getLongitude() >= -88.2272 && p.getLongitude() <= -88.2271)) {
      imageView.setImageResource(R.drawable.daniel_odicho);
    } else if ((p.getLatitude() >= 40.1094 && p.getLatitude() <= 40.1095)
        && (p.getLongitude() >= -88.2312 && p.getLongitude() <= -88.2311)) {
      imageView.setImageResource(R.drawable.peter_chen);
    } else if ((p.getLatitude() >= 40.1149 && p.getLatitude() <= 40.1150)
        && (p.getLongitude() >= -88.2282 && p.getLongitude() <= -88.2281)) {
      imageView.setImageResource(R.drawable.zetai_liu);
    } else if ((p.getLatitude() >= 40.0954 && p.getLatitude() <= 40.0955)
        && (p.getLongitude() >= -88.2204 && p.getLongitude() <= -88.2203)) {
      imageView.setImageResource(R.drawable.geoffrey_challen);
    } else if ((p.getLatitude() >= 40.1103 && p.getLatitude() <= 40.1104)
        && (p.getLongitude() >= -88.2301 && p.getLongitude() <= -88.2300)) {
      imageView.setImageResource(R.drawable.arijit_ghosh_chowdhury);
    } else if ((p.getLatitude() >= 40.1165 && p.getLatitude() <= 40.1166)
        && (p.getLongitude() >= -88.2293 && p.getLongitude() <= -88.2292)) {
      imageView.setImageResource(R.drawable.dj_figueiredo);
    } else if ((p.getLatitude() >= 40.1091 && p.getLatitude() <= 40.1092)
        && (p.getLongitude() >= -88.2313 && p.getLongitude() <= -88.2312)) {
      imageView.setImageResource(R.drawable.niharika_bhattacharjee);
    } else if ((p.getLatitude() >= 40.1291 && p.getLatitude() <= 40.1292)
        && (p.getLongitude() >= -88.2386 && p.getLongitude() <= -88.2385)) {
      imageView.setImageResource(R.drawable.kaiwen_ren);
    } else if ((p.getLatitude() >= 40.1082 && p.getLatitude() <= 40.1083)
        && (p.getLongitude() >= -88.2293 && p.getLongitude() <= -88.2292)) {
      imageView.setImageResource(R.drawable.atharv_chandratre);
    } else if ((p.getLatitude() >= 40.1104 && p.getLatitude() <= 40.1105)
        && (p.getLongitude() >= -88.2328 && p.getLongitude() <= -88.2327)) {
      imageView.setImageResource(R.drawable.yadu_reddy);
    } else if ((p.getLatitude() >= 40.1126 && p.getLatitude() <= 40.1127)
        && (p.getLongitude() >= -88.2094 && p.getLongitude() <= -88.2093)) {
      imageView.setImageResource(R.drawable.di_liang);
    } else if ((p.getLatitude() >= 40.1113 && p.getLatitude() <= 40.1114)
        && (p.getLongitude() >= -88.2292 && p.getLongitude() <= -88.2291)) {
      imageView.setImageResource(R.drawable.jane_liu);
    } else if ((p.getLatitude() >= 40.1081 && p.getLatitude() <= 40.1082)
        && (p.getLongitude() >= -88.2229 && p.getLongitude() <= -88.2228)) {
      imageView.setImageResource(R.drawable.aden_krakman);
    } else if ((p.getLatitude() >= 40.1104 && p.getLatitude() <= 40.1105)
        && (p.getLongitude() >= -88.2325 && p.getLongitude() <= -88.2324)) {
      imageView.setImageResource(R.drawable.raul_higareda);
    } else if ((p.getLatitude() >= 40.1123 && p.getLatitude() <= 40.1124)
        && (p.getLongitude() >= -88.2270 && p.getLongitude() <= -88.2269)) {
      imageView.setImageResource(R.drawable.anushree_tibrewal);
    } else if ((p.getLatitude() >= 40.1103 && p.getLatitude() <= 40.1104)
        && (p.getLongitude() >= -88.2362 && p.getLongitude() <= -88.2361)) {
      imageView.setImageResource(R.drawable.henry_tang);
    } else if ((p.getLatitude() >= 40.1104 && p.getLatitude() <= 40.1105)
        && (p.getLongitude() >= -88.2332 && p.getLongitude() <= -88.2331)) {
      imageView.setImageResource(R.drawable.paul_brodnansky);
    } else if ((p.getLatitude() >= 40.1173 && p.getLatitude() <= 40.1174)
        && (p.getLongitude() >= -88.2434 && p.getLongitude() <= -88.2433)) {
      imageView.setImageResource(R.drawable.prerana_singh);
    } else if ((p.getLatitude() >= 40.1135 && p.getLatitude() <= 40.1136)
        && (p.getLongitude() >= -88.2380 && p.getLongitude() <= -88.2379)) {
      imageView.setImageResource(R.drawable.chris_sahyouni);
    } else if ((p.getLatitude() >= 40.1035 && p.getLatitude() <= 40.1036)
        && (p.getLongitude() >= -88.2347 && p.getLongitude() <= -88.2346)) {
      imageView.setImageResource(R.drawable.justin_huang);
    } else if ((p.getLatitude() >= 40.1172 && p.getLatitude() <= 40.1173)
        && (p.getLongitude() >= -88.2161 && p.getLongitude() <= -88.2160)) {
      imageView.setImageResource(R.drawable.bilal_karim);
    } else if ((p.getLatitude() >= 40.1010 && p.getLatitude() <= 40.1011)
        && (p.getLongitude() >= -88.2356 && p.getLongitude() <= -88.2355)) {
      imageView.setImageResource(R.drawable.ram_goenka);
    } else if ((p.getLatitude() >= 40.1105 && p.getLatitude() <= 40.1106)
        && (p.getLongitude() >= -88.2340 && p.getLongitude() <= -88.2339)) {
      imageView.setImageResource(R.drawable.jimmy_berg);
    } else if ((p.getLatitude() >= 40.1105 && p.getLatitude() <= 40.1106)
        && (p.getLongitude() >= -88.2306 && p.getLongitude() <= -88.2305)) {
      imageView.setImageResource(R.drawable.dingsen_shi);
    } else if ((p.getLatitude() >= 40.1074 && p.getLatitude() <= 40.1075)
        && (p.getLongitude() >= -88.2270 && p.getLongitude() <= -88.2269)) {
      imageView.setImageResource(R.drawable.ajay_karthikeyan);
    } else if ((p.getLatitude() >= 40.1191 && p.getLatitude() <= 40.1192)
        && (p.getLongitude() >= -88.2438 && p.getLongitude() <= -88.2437)) {
      imageView.setImageResource(R.drawable.akash_kumar);
    } else if ((p.getLatitude() >= 40.1101 && p.getLatitude() <= 40.1102)
        && (p.getLongitude() >= -88.2314 && p.getLongitude() <= -88.2313)) {
      imageView.setImageResource(R.drawable.akash_kumar);
    } else if ((p.getLatitude() >= 40.1124 && p.getLatitude() <= 40.1125)
        && (p.getLongitude() >= -88.2268 && p.getLongitude() <= -88.2267)) {
      imageView.setImageResource(R.drawable.justin_bai);
    } else if ((p.getLatitude() >= 40.1096 && p.getLatitude() <= 40.1097)
        && (p.getLongitude() >= -88.2057 && p.getLongitude() <= -88.2056)) {
      imageView.setImageResource(R.drawable.yana_zhao);
    } else if ((p.getLatitude() >= 40.0961 && p.getLatitude() <= 40.0962)
        && (p.getLongitude() >= -88.2582 && p.getLongitude() <= -88.2581)) {
      imageView.setImageResource(R.drawable.akash_kumar);
    } else if ((p.getLatitude() >= 40.1104 && p.getLatitude() <= 40.1105)
        && (p.getLongitude() >= -88.2310 && p.getLongitude() <= -88.2309)) {
      imageView.setImageResource(R.drawable.yasmine_munoz);
    } else if ((p.getLatitude() >= 40.0995 && p.getLatitude() <= 40.0996)
        && (p.getLongitude() >= -88.2505 && p.getLongitude() <= -88.2504)) {
      imageView.setImageResource(R.drawable.akash_kumar);
    } else if ((p.getLatitude() >= 40.1099 && p.getLatitude() <= 40.1100)
        && (p.getLongitude() >= -88.2292 && p.getLongitude() <= -88.2291)) {
      imageView.setImageResource(R.drawable.akash_kumar);
    } else if ((p.getLatitude() >= 40.1064 && p.getLatitude() <= 40.1065)
        && (p.getLongitude() >= -88.2214 && p.getLongitude() <= -88.2213)) {
      imageView.setImageResource(R.drawable.nancy_jia);
    } else if ((p.getLatitude() >= 40.1157 && p.getLatitude() <= 40.1158)
        && (p.getLongitude() >= -88.2395 && p.getLongitude() <= -88.2384)) {
      imageView.setImageResource(R.drawable.carlos_conley);
    } else if ((p.getLatitude() >= 40.1124 && p.getLatitude() <= 40.1125)
        && (p.getLongitude() >= -88.2281 && p.getLongitude() <= -88.2280)) {
      imageView.setImageResource(R.drawable.aryan_patel);
    } else if ((p.getLatitude() >= 40.1104 && p.getLatitude() <= 40.1105)
        && (p.getLongitude() >= -88.2299 && p.getLongitude() <= -88.2298)) {
      imageView.setImageResource(R.drawable.kevin_liu);
    } else if ((p.getLatitude() >= 40.1102 && p.getLatitude() <= 40.1103)
        && (p.getLongitude() >= -88.2292 && p.getLongitude() <= -88.2291)) {
      imageView.setImageResource(R.drawable.armin_rafieyan);
    } else if ((p.getLatitude() >= 40.1102 && p.getLatitude() <= 40.1103)
        && (p.getLongitude() >= -88.2297 && p.getLongitude() <= -88.2296)) {
      imageView.setImageResource(R.drawable.tina_dou);
    } else {
      imageView.setImageResource(R.drawable.akash_kumar);
    }
    return false;
  }

  @Override
  public boolean longPressHelper(final GeoPoint p) {
    Log.d(TAG, " longPress");
    String latitude = Double.toString(p.getLatitude());
    String longitude = Double.toString(p.getLongitude());
    Intent launchAddFavoritePlace = new Intent(this, AddPlaceActivity.class);
    launchAddFavoritePlace.putExtra("latitude", latitude);
    launchAddFavoritePlace.putExtra("longitude", longitude);
    startActivity(launchAddFavoritePlace);
    return false;
  }
}
