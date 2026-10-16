package com.example.views;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reading the packaged test sources the views show. */
class TestSourceTest {

    @Test
    void methodNamesBecomeSentences() {
        assertEquals("Placing an order confirms stores and opens the list",
                TestSource.sentence(
                        "placingAnOrderConfirmsStoresAndOpensTheList"));
        assertEquals("Quantity must be positive",
                TestSource.sentence("quantityMustBePositive"));
    }

    @Test
    void findsEveryTestMethodWithItsAnnotationsAndBody() {
        List<TestSource.TestMethod> methods = TestSource
                .methods("orders/QuoteCalculatorTest");

        assertEquals(
                List.of("volumeAndBusinessDiscounts", "smallOrderPaysShipping",
                        "shippingIsFreeFromOneHundredAfterDiscount",
                        "discountCanDropAnOrderBelowFreeShipping",
                        "quantityMustBePositive"),
                methods.stream().map(TestSource.TestMethod::name).toList());
        String code = methods.get(1).code();
        assertTrue(code.startsWith("@Test\nvoid smallOrderPaysShipping() {"),
                code);
        assertTrue(code.endsWith("}"), code);
        // Annotations spread over several lines stay with their method.
        assertTrue(methods.getFirst().code().startsWith("@ParameterizedTest"));
    }

    @Test
    void endToEndTestsAreFoundToo() {
        assertEquals(List.of("placedOrderAppearsAtTheTopOfTheList"),
                TestSource.methods("uc8/CheckoutIT").stream()
                        .map(TestSource.TestMethod::name).toList());
    }

    @Test
    void bracesInStringsCommentsAndTextBlocksDoNotEndTheMethod() {
        List<String> source = List.of("class T {", "    @Test",
                "    void braces() {", "        assertEquals(\"}\", close());",
                "        char c = '}'; // }", "        /* } */",
                "        String block = \"\"\"", "            }",
                "            \"\"\";", "    }", "", "    @Test",
                "    void next() {", "    }", "}");

        List<TestSource.TestMethod> methods = TestSource.methods(source);

        assertEquals(List.of("braces", "next"),
                methods.stream().map(TestSource.TestMethod::name).toList());
        assertTrue(methods.getFirst().code().endsWith("\"\"\";\n}"),
                methods.getFirst().code());
    }

    @Test
    void annotationsAboveTheTestAnnotationAreShown() {
        List<String> source = List.of("class T {",
                "    @WithMockUser(roles = \"ADMIN\")", "    @Test",
                "    void admin() {", "    }", "}");

        assertEquals(
                "@WithMockUser(roles = \"ADMIN\")\n@Test\n"
                        + "void admin() {\n}",
                TestSource.methods(source).getFirst().code());
    }

    @Test
    void annotationWithoutAMethodIsSkipped() {
        assertEquals(List.of(),
                TestSource.methods(List.of("class T {", "    @Test", "}")));
    }

    @Test
    void unknownSourceIsAnError() {
        assertThrows(IllegalArgumentException.class,
                () -> TestSource.methods("uc99/NoSuchTest"));
    }
}
