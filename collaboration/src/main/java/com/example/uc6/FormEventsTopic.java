package com.example.uc6;

import com.example.collab.FormState;

import org.springframework.stereotype.Component;

/** UC6's shared form, kept apart from UC5's so the logs stay readable. */
@Component
public class FormEventsTopic {

    public static final String NAME = "name";
    public static final String EMAIL = "email";

    private final FormState form = new FormState();

    public FormState form() {
        return form;
    }
}
