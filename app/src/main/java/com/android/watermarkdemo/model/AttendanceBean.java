package com.android.watermarkdemo.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

/**
 * 考勤
 */

@Table("Attendance")
public class AttendanceBean implements Parcelable{

    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private int id;
    private String datetime;
    private String address;
    private String pictureListJson;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.datetime);
        dest.writeString(this.address);
        dest.writeString(this.pictureListJson);
    }

    public AttendanceBean() {
    }

    protected AttendanceBean(Parcel in) {
        this.id = in.readInt();
        this.datetime = in.readString();
        this.address = in.readString();
        this.pictureListJson = in.readString();
    }

    public static final Creator<AttendanceBean> CREATOR = new Creator<AttendanceBean>() {
        @Override
        public AttendanceBean createFromParcel(Parcel source) {
            return new AttendanceBean(source);
        }

        @Override
        public AttendanceBean[] newArray(int size) {
            return new AttendanceBean[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPictureListJson() {
        return pictureListJson;
    }

    public void setPictureListJson(String pictureListJson) {
        this.pictureListJson = pictureListJson;
    }
}
