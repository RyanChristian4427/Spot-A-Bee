package com.assignment.spotabee.imagerecognition;
/**
 * Made by: C1769948
 */

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import okhttp3.OkHttpClient;


/**
 * Purpose to provide a ClarifaiClient that can be used to make
 * image recognition requests
 */

public abstract class ClarifaiClientGenerator {
    private static final Logger LOGGER = Logger.getLogger( ClarifaiClientGenerator.class.getName());

    /**
     *
     * @param  apiKey - String value API key from Clarifai
     * @return
     */
    public static ClarifaiClient generate(String apiKey){
        ClarifaiClient client= new ClarifaiBuilder(apiKey)
                .client(new OkHttpClient.Builder()
                        .readTimeout(30, TimeUnit.SECONDS) // Increase timeout for poor mobile networks
                        .build()
                )
                .buildSync();
        return client;

    }
}