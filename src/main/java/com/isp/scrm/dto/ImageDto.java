package com.isp.scrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ImageDto {

    @NotBlank
    private String image;

    @NotBlank
    @Pattern(regexp = "P|A|D", message = "tag must be P, A or D")
    private String tag;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
