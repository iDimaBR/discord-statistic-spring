package com.github.idimabr.controllers;

import com.github.idimabr.model.DiscordStatistic;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.util.HttpConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.asynchttpclient.*;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.*;
import static org.asynchttpclient.Dsl.asyncHttpClient;

@Controller
public class WebController {

    private Map<UUID, DiscordStatistic> loggeds = new HashMap<>();
    @Value("${app.discord.token}")
    private String discordToken;

    @GetMapping("/")
    public String index(ModelMap model){
        model.addAttribute("salve", "Treinaweb");
        return "home";
    }

    @GetMapping("/redirect")
    public ResponseEntity<Void> login() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("https://discord.com/api/oauth2/authorize?client_id=1094349621440020571&redirect_uri=http%3A%2F%2F127.0.0.1%3A8080%2Fstatus&response_type=code&scope=identify%20guilds%20email"))
                .build();
    }

    @RequestMapping(value = {"/status/", "/status"}, method = RequestMethod.GET)
    public String logged(ModelMap model, @RequestParam Map<String,String> params, HttpServletResponse response) {

        final String authSession = response.getHeader("authSession");
        if(!params.containsKey("code")){
            if(authSession == null){
                return "home";
            }else{
                model.put("statistics", loggeds.get(authSession));
                return "status";
            }
        }

        final AsyncHttpClient asyncHttpClient = asyncHttpClient();

        try {
            Request request = new RequestBuilder(HttpConstants.Methods.POST)
                    .setUrl("https://discordapp.com/api/oauth2/token")
                    .addFormParam("code", params.get("code"))
                    .addFormParam("client_id", "1094349621440020571")
                    .addFormParam("client_secret", discordToken)
                    .addFormParam("grant_type", "authorization_code")
                    .addFormParam("redirect_uri", "http://127.0.0.1:8080/status")
                    .addFormParam("scope", "identify%20guilds%20email")
                    .build();

            final String returnBody = asyncHttpClient.executeRequest(request).get().getResponseBody();
            final Map<String, Object> returnMap = new Gson().fromJson(returnBody, new TypeToken<HashMap<String, String>>() {
            }.getType());

            Request discordGet = new RequestBuilder(HttpConstants.Methods.GET)
                    .setUrl("https://discordapp.com/api/users/@me")
                    .addHeader("Authorization", "Bearer " + returnMap.get("access_token"))
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            final String responseBody = asyncHttpClient.executeRequest(discordGet).get().getResponseBody();
            Map<String, Object> responseMap = new Gson().fromJson(responseBody, new TypeToken<HashMap<String, String>>(){}.getType());
            if(responseMap.isEmpty()){
                return "home";
            }

            final DiscordStatistic statistic = new DiscordStatistic(
                    Long.parseLong((String) responseMap.get("id")),
                    (String) responseMap.get("username"),
                    (String) responseMap.get("avatar"),
                    (String) responseMap.get("discriminator"),
                    (String) responseMap.get("email"),
                    Boolean.parseBoolean((String) responseMap.get("verified"))
            );

            final UUID sessionUser = UUID.randomUUID();

            loggeds.put(sessionUser, statistic);
            model.put("statistics", statistic);
            response.addHeader("authSession", sessionUser.toString());
            return "status";
        } catch (Exception ex) {
            return "home";
        }
    }
}
