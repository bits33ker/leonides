package com.herod.rtp;

public class RtpCodec {
    public static final int LINEAR_MEDIA_FORMAT = -1;
    private RtpMime mime;
    private int media;
    private int freq;
    private int channels = 1;

    public RtpCodec() {
        this.mime = RtpMime.PCM;
        this.media = 0;
        this.freq = 8000;
    }
    public RtpCodec(RtpMime mime, int mediaFormat, int freq) {
        this.mime = mime;
        this.media = mediaFormat;
        this.freq = freq;
    }

    public RtpCodec(RtpMime mime, int mediaFormat, int freq, int channels) {
        this.mime = mime;
        this.media = mediaFormat;
        this.freq = freq;
        this.channels = channels;
    }

    public RtpMime getMime() {
        return this.mime;
    }

    public int getMedia() {
        return this.media;
    }

    public int getFreq() {
        return this.freq;
    }

    public int getChannels() {
        return this.channels;
    }

    public MediaType getMediaType() {
        return this.mime.getMediaType();
    }

    public static RtpCodec buildFromString(String value) throws IllegalArgumentException, RtpCodecException {
        String[] parts = value.split(" ");
        if (parts.length > 1) {
            int mediaFormat = Integer.parseInt(parts[0]);
            String[] mediaParts = parts[1].split("/");
            if (mediaParts.length > 1) {
                String mime = mediaParts[0];
                int freq = Integer.parseInt(mediaParts[1]);
                RtpMime rtpMime = RtpMime.getByName(mime.toUpperCase());
                return new RtpCodec(rtpMime, mediaFormat, freq);
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
}
