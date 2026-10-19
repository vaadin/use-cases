package com.example.uc5;

import com.example.collab.FormState;

import org.springframework.stereotype.Component;

/**
 * UC5's shared form. Application-scoped, so every session and every simulated
 * peer edits the same customer record.
 */
@Component
public class FormTopic {

    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String ADDRESS = "address";

    private final FormState form = new FormState();

    public FormState form() {
        return form;
    }
}
