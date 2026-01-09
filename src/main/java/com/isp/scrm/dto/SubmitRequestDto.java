package com.isp.scrm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class SubmitRequestDto {

    @NotBlank
    private String codeIsp;

    @NotBlank
    private String codeCia;

    @NotNull
    private Boolean raci = false;

    @NotEmpty
    @Valid
    private List<ImageDto> images;

    public String getCodeIsp() {
        return codeIsp;
    }

    public void setCodeIsp(String codeIsp) {
        this.codeIsp = codeIsp;
    }

    public String getCodeCia() {
        return codeCia;
    }

    public void setCodeCia(String codeCia) {
        this.codeCia = codeCia;
    }

    public Boolean getRaci() {
        return raci;
    }

    public void setRaci(Boolean raci) {
        this.raci = raci;
    }

    public List<ImageDto> getImages() {
        return images;
    }

    public void setImages(List<ImageDto> images) {
        this.images = images;
    }
}