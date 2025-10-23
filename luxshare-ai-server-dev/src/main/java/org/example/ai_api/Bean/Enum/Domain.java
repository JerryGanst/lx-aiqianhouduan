package org.example.ai_api.Bean.Enum;

import lombok.Getter;

@Getter
public enum Domain {
    IT("IT"),
    HR("HR Infos"),
    OTHER("Other");

    private final String domain;

    Domain(String domain) {
        this.domain = domain;
    }
}
