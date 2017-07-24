package de.tudarmstadt.informatik.tk.silentmusicparty.entities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * Class that models the aspects of a person that are relevant for a party.
 * <p>
 * Created by chrisbe on 03.01.2017.
 */
public class Person implements Parcelable, Serializable {

    public static final int FEMALE = 0;

    private String uid;
    private String name;
    private String birthday;
    private int height;
    private int gender;
    private String location;
    private String message;


    /*-----------------CONSTRUCTOR-------------------*/
    /**
     * Simple constructor.
     * @param uid user id
     * @param name user name
     */
    public Person(String uid, String name){
        this.uid = uid;
        this.name = name;
    }

    /**
     * Full constructor
     * @param uid user id
     * @param name
     * @param birthday
     * @param height
     * @param gender
     * @param location
     * @param message
     */
    public Person(String uid, String name, String birthday, int height, int gender, String location, String message) {
        this.uid = uid;
        this.name = name;
        this.birthday = birthday;
        this.height = height;
        this.gender = gender;
        this.location = location;
        this.message = message;
    }
    /*-----------------CONSTRUCTOR end-------------------*/

    /*-----------------PARCELABLE STUFF-------------------*/
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uid);
        parcel.writeString(name);
        parcel.writeString(birthday);
        parcel.writeInt(height);
        parcel.writeInt(gender);
        parcel.writeString(location);
        parcel.writeString(message);
    }
    public static final Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<Person>() {
        public Person createFromParcel(Parcel in) {
            return new Person(in);
        }

        public Person[] newArray(int size) {
            return new Person[size];
        }
    };
    private Person(Parcel in) {
        uid = in.readString();
        name = in.readString();
        birthday = in.readString();
        height = in.readInt();
        gender = in.readInt();
        location = in.readString();
        message = in.readString();
    }
    /*-----------------PARCELABLE STUFF end-------------------*/


    /*-----------------GETTERS-------------------*/
    /**
     * Gets the user id.
     * @return
     */
    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getBirthday() {
        return birthday;
    }

    public int getHeight() {
        return height;
    }

    public String getLocation() {
        return location;
    }

    public String getMessage() {
        return message;
    }

    public String getGender() { return (gender == FEMALE) ? "female" : "male";}

}
