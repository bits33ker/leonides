package com.herod.rtp;

public enum RtpMime {
    //https://en.wikipedia.org/wiki/RTP_payload_formats
    PCMU("PCMU", MediaType.Audio),
    CELP("CELP", MediaType.Audio),
    G721("G721", MediaType.Audio),
    GSM("GSM", MediaType.Audio),
    G723("G723", MediaType.Audio),
    PCM("PCM", MediaType.Audio),
    PCMA("PCMA", MediaType.Audio),
    G729("G729", MediaType.Audio),
    OPUS("OPUS", MediaType.Audio),
    ILBC("ILBC", MediaType.Audio),
    VP8("VP8", MediaType.Video),
    TELEPHONE_EVENT("TELEPHONE-EVENT", MediaType.Audio);

    private String name;
    private MediaType mediaType;

    private RtpMime(String name, MediaType mediaType) {
        this.name = name;
        this.mediaType = mediaType;
    }

    public String getName() {
        return this.name;
    }

    public MediaType getMediaType() {
        return this.mediaType;
    }
    public static RtpMime getByName(String name) throws RtpCodecException {
        switch(name){
            case "PCMU":
                return PCM;
            case "PCMA":
                return PCMA;
            case "GSM":
                return GSM;
            case "CELP":
                return CELP;
            case "G721":
                return G721;
            case "G723":
                return G723;
            case "G729":
                return G729;
            case "OPUS":
                return OPUS;
            case "ILBC":
                return ILBC;
            case "VP8":
                return VP8;
        }
        throw new RtpCodecException("Codec name ERROR");
    }
}
