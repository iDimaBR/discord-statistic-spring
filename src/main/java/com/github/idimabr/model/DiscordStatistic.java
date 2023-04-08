package com.github.idimabr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor @Getter @Setter
public class DiscordStatistic {

    private long discordId;
    private String username;
    private String avatar;
    private String discriminator;

    private String email;
    private boolean verified;

}
