package bo.young.myfoodrecords.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Realm Object for Food Model
 */
public class Food extends RealmObject {

    @PrimaryKey
    private int id;

    private String name;
    private float rating;

    private String date;
    private String foodType;
    private String photoPath;
    private Boolean isFavorite = false;
    private PlaceModel placeModel;
    private String description;
    private Boolean isFromGallery;

    public Boolean getFromGallery() {
        return isFromGallery;
    }

    public void setFromGallery(Boolean fromGallery) {
        isFromGallery = fromGallery;
    }

    public Food(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public Boolean getFavorite() {
        return isFavorite;
    }

    public void setFavorite(Boolean favorite) {
        isFavorite = favorite;
    }

    public PlaceModel getPlaceModel() {
        return placeModel;
    }

    public void setPlaceModel(PlaceModel placeModel) {
        this.placeModel = placeModel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
