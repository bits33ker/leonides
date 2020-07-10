package com.herod.rtp;

public enum MediaType {
    Audio(0, "Audio"),
    Video(1, "Video"),
    AudioVideo(2, "Audio-Video"),
    Screen(3, "Screen");

    public static final String MEDIA_TYPE_KEY = "mediaType";
    private int code;
    private String description;

    private MediaType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }
}
