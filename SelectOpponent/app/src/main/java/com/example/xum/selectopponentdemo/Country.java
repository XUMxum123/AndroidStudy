package com.example.xum.selectopponentdemo;

import java.io.Serializable;

/**
 * Created by meng.xu on 2017/12/9.
 */

public class Country implements Serializable,Cloneable {

    private static final long serialVersionUID = 4524711029775144836L;
    private String name;
    private int drawableInt;
    private int drawableSwitchInt;
	private String scrollMark;

    private String firstImageName;
    private String secondImageName;

    public Country() {
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setDrawableInt(int drawableInt) {
        this.drawableInt = drawableInt;
    }

    public int getDrawableInt() {
        return drawableInt;
    }

    public void setDrawableSwitchInt(int drawableSwitchInt) {
        this.drawableSwitchInt = drawableSwitchInt;
    }

    public int getDrawableSwitchInt() {
        return drawableSwitchInt;
    }

	public void setScrollMark(String scrollMark) {
		this.scrollMark = scrollMark;
	}

	public String getScrollMark() {
		return scrollMark;
	}

    public void setFirstImageName(String firstImageName) {
        this.firstImageName = firstImageName;
    }

    public String getFirstImageName() {
        return firstImageName;
    }

    public void setSecondImageName(String secondImageName) {
        this.secondImageName = secondImageName;
    }

    public String getSecondImageName() {
        return secondImageName;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}

