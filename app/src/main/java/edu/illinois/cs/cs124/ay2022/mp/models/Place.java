package edu.illinois.cs.cs124.ay2022.mp.models;

import java.util.ArrayList;
import java.util.List;

/*
 * Model storing information about a place retrieved from the backend server.
 *
 * You will need to understand some of the code in this file and make changes starting with MP1.
 */
@SuppressWarnings("unused")
public final class Place {
  /*
   * The Jackson JSON serialization library that we are using requires an empty constructor.
   * So don't remove this!
   */
  public Place() {}

  public Place(
      final String setId,
      final String setName,
      final double setLatitude,
      final double setLongitude,
      final String setNpg,
      final String setDescription) {
    id = setId;
    name = setName;
    latitude = setLatitude;
    longitude = setLongitude;
    description = setDescription;
    npg = setNpg;
  }

  // ID of the place
  private String npg;
  private String id;

  public String getId() {
    return id;
  }

  // Name of the person who submitted this favorite place
  private String name;

  public String getName() {
    return name;
  }

  // Latitude and longitude of the place
  private double latitude = 99999.0;

  public double getLatitude() {
    return latitude;
  }

  private double longitude = 99999.0;

  public double getLongitude() {
    return longitude;
  }

  // Description of the place
  private String description;

  public String getDescription() {
    return description;
  }

  public static List<Place> search(final List<Place> places, final String search) {
    if (places == null || search == null) {
      throw new IllegalArgumentException();
    }
    if (places.size() == 0 || search.length() == 0 || search.equals(" ")) {
      return places;
    }
    List<Place> copy = new ArrayList<>();
    String trimmed = search.trim();
    String trimmed1 = trimmed.toLowerCase();
    for (Place o : places) {
      String desc = o.description;
      String desc1 = desc.replace('.', ' ');
      String desc2 = desc1.replace('!', ' ');
      String desc3 = desc2.replace('?', ' ');
      String desc4 = desc3.replace(',', ' ');
      String desc5 = desc4.replace(':', ' ');
      String desc6 = desc5.replace(';', ' ');
      String desc7 = desc6.replace('/', ' ');
      String desc8 = desc7.replace("-", "");
      String desc9 = desc8.replace("(", "");
      String desc10 = desc9.replace(")", "");
      String desc11 = desc10.replace("'", "");
      String desc12 = desc11.toLowerCase();
      String[] desc13 = desc12.split(" ");
      for (String b : desc13) {
        if (trimmed1.equals(b)) {
          if (!copy.contains(o)) {
            copy.add(o);
          }
        }
      }
    }
    return copy;
  }
}
