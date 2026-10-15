package com.example.uc12;

import java.io.IOException;
import java.util.Map;

import com.example.uc12.AddressChangeForm.Field;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressChangeFormTest {

    @Test
    void theBlankFormHasAFieldForEachQuestion() throws IOException {
        try (PDDocument document = Loader.loadPDF(AddressChangeForm.blank())) {
            PDAcroForm form = document.getDocumentCatalog().getAcroForm();
            assertEquals(Field.values().length, form.getFields().size());
        }
    }

    @Test
    void theFilledFormShowsTheAnswersAndHasNoFieldsLeft() throws IOException {
        byte[] filled = AddressChangeForm.fill(Map.of(Field.NAME, "Maria Lind",
                Field.NEW_ADDRESS, "Kauppakatu 3\n40100 Jyväskylä"));

        try (PDDocument document = Loader.loadPDF(filled)) {
            PDAcroForm form = document.getDocumentCatalog().getAcroForm();
            assertTrue(form == null || form.getFields().isEmpty(),
                    "the answers can no longer be edited");
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Maria Lind"), text);
            assertTrue(text.contains("40100 Jyväskylä"), text);
        }
    }
}
