package com.example.documents;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * The made-up documents the use cases show. Acme is a fictional company; the
 * documents are generated on the fly, so nothing binary is committed.
 */
public final class SampleDocuments {

    /** Millimetres to PDF points. */
    private static final float MM = 72 / 25.4f;

    private SampleDocuments() {
    }

    /** A line of an invoice. */
    public record InvoiceLine(String description, int quantity,
            BigDecimal unitPrice) {

        public BigDecimal total() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /** An invoice waiting to be sent. */
    public record Invoice(String number, String customer, List<String> address,
            List<InvoiceLine> lines) {

        public BigDecimal total() {
            return lines.stream().map(InvoiceLine::total)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public String fileName() {
            return "invoice-" + number + ".pdf";
        }
    }

    /** The invoices of UC1. */
    public static final List<Invoice> INVOICES = List.of(
            new Invoice("2026-0412", "Nordwind Logistics",
                    List.of("Nordwind Logistics GmbH", "Hafenstrasse 12",
                            "20457 Hamburg", "Germany"),
                    List.of(new InvoiceLine("Fleet tracking licence", 25,
                            new BigDecimal("19.00")),
                            new InvoiceLine("Onboarding workshop", 1,
                                    new BigDecimal("850.00")))),
            new Invoice("2026-0413", "Lumen Dental",
                    List.of("Lumen Dental Oy", "Mannerheimintie 40",
                            "00100 Helsinki", "Finland"),
                    List.of(new InvoiceLine("Booking system, monthly", 3,
                            new BigDecimal("120.00")),
                            new InvoiceLine("SMS reminders (1000)", 4,
                                    new BigDecimal("35.00")),
                            new InvoiceLine("Data migration", 1,
                                    new BigDecimal("400.00")))),
            new Invoice("2026-0414", "Casa Verde",
                    List.of("Casa Verde S.L.", "Calle Mayor 7", "28013 Madrid",
                            "Spain"),
                    List.of(new InvoiceLine("Web shop hosting, yearly", 1,
                            new BigDecimal("1200.00")))));

    /**
     * Formats an amount the same way in the documents and in the views, so the
     * two can be compared.
     *
     * @param amount
     *            the amount
     * @return the amount with two decimals and the currency
     */
    public static String money(BigDecimal amount) {
        return String.format(Locale.ROOT, "%,.2f EUR", amount);
    }

    /**
     * Renders an invoice.
     *
     * @param invoice
     *            the invoice
     * @return the invoice as a PDF
     */
    public static Pdf invoice(Invoice invoice) {
        PdfBuilder pdf = new PdfBuilder("Invoice " + invoice.number());
        pdf.paragraph("Acme Software Ltd, 1 Harbour Lane, Dublin, Ireland");
        invoice.address().forEach(pdf::paragraph);
        pdf.heading("Items");
        pdf.boldRow("Description", "Amount");
        for (InvoiceLine line : invoice.lines()) {
            pdf.row(line.quantity() + " x " + line.description(),
                    money(line.total()));
        }
        pdf.boldRow("Total due", money(invoice.total()));
        pdf.heading("Payment");
        pdf.paragraph("Please pay within 14 days to IBAN IE29 AIBK 9311 5212 "
                + "3456 78 and mark the payment with the invoice number.");
        return new Pdf(invoice.fileName(), pdf.build());
    }

    /** The terms of service of UC2: several pages the user has to read. */
    public static Pdf termsOfService() {
        PdfBuilder pdf = new PdfBuilder("Acme Cloud — Terms of Service")
                .bodySize(12).pageSize(PDRectangle.A4, 72);
        pdf.paragraph("Version 4.2, valid from 1 October 2026. Please read "
                + "these terms carefully. By accepting them you enter into a "
                + "binding agreement with Acme Software Ltd.");
        section(pdf, "1. Who we are",
                "Acme Software Ltd provides Acme Cloud, an online service for "
                        + "storing, sharing and editing business documents. In "
                        + "these terms, \"we\" means Acme Software Ltd and "
                        + "\"you\" means the organisation that signs up, and "
                        + "the people it lets use the service.",
                "If you accept these terms for an organisation, you confirm "
                        + "that you may act on its behalf.");
        section(pdf, "2. Your account",
                "You need an account to use the service. Keep your password "
                        + "secret and turn on two-step sign-in. Tell us at "
                        + "once if you think someone else has used your "
                        + "account.",
                "You are responsible for what happens in your account, "
                        + "including what the people you invite do there. "
                        + "Remove access for people who leave your "
                        + "organisation.");
        section(pdf, "3. Using the service",
                "You may use the service for your business. You must not use "
                        + "it to break the law, to store malware, to send spam "
                        + "or to try to get into accounts or systems that are "
                        + "not yours.",
                "We may limit or suspend an account that puts the service or "
                        + "other customers at risk. We will tell you why, and "
                        + "give you a chance to fix the problem first when we "
                        + "can.");
        section(pdf, "4. Your content",
                "What you store in the service stays yours. You give us only "
                        + "the rights we need to run the service: to store "
                        + "your content, to make copies for backups, and to "
                        + "show it to the people you share it with.",
                "We do not read your content, sell it or use it to train "
                        + "models, unless you ask us to, for example to "
                        + "answer a support request.");
        section(pdf, "5. Fees",
                "The prices are on our website. Fees are charged per user and "
                        + "month, in advance. Prices do not include VAT.",
                "We may change the prices with 60 days' notice. A change "
                        + "applies from your next billing period. If you do "
                        + "not accept it, you may cancel before it applies.");
        section(pdf, "6. Availability",
                "We aim to keep the service available 99.9% of the time each "
                        + "month, not counting maintenance we announce at "
                        + "least five days ahead.",
                "If we miss that target, you get a credit on your next "
                        + "invoice, as described in the service level "
                        + "agreement. The credit is your only remedy for the "
                        + "missed target.");
        section(pdf, "7. Privacy",
                "We process personal data on your behalf as described in the "
                        + "data processing agreement, which is part of these "
                        + "terms. Our data centres are in the European Union.",
                "We use sub-processors only as listed on our website, and we "
                        + "tell you 30 days before we add a new one.");
        section(pdf, "8. Liability",
                "Neither of us is liable for indirect losses, such as lost "
                        + "profits or lost business. Our total liability in a "
                        + "year is limited to the fees you paid us in that "
                        + "year.",
                "These limits do not apply to losses caused on purpose or by "
                        + "gross negligence, or where the law does not allow "
                        + "them.");
        section(pdf, "9. Ending the agreement",
                "You may cancel at any time; the cancellation takes effect at "
                        + "the end of the billing period. We may end the "
                        + "agreement with 90 days' notice, or at once if you "
                        + "seriously break these terms.",
                "After the agreement ends you have 30 days to export your "
                        + "content. Then we delete it, including from backups "
                        + "within a further 60 days.");
        section(pdf, "10. Changes to these terms",
                "We may change these terms. We tell you about important "
                        + "changes at least 30 days before they apply, and ask "
                        + "you to accept them again.",
                "These terms are governed by the laws of Ireland. Courts in "
                        + "Dublin handle any dispute that we can not settle "
                        + "between us.");
        return new Pdf("acme-cloud-terms.pdf", pdf.build());
    }

    /** The help topics of the user guide, and where the guide covers them. */
    public record Manual(Pdf pdf, Map<String, Integer> chapterPages) {
    }

    /**
     * The user guide of UC4: chapters with bookmarks, so the viewer's sidebar
     * shows an outline.
     */
    public static Manual userGuide() {
        PdfBuilder pdf = new PdfBuilder("Acme Time Tracker — User Guide")
                .withOutline().chaptersOnNewPages();
        pdf.paragraph("This guide explains how to record your working time, "
                + "submit timesheets and get them approved. Each chapter "
                + "stands on its own, so you can go straight to the one you "
                + "need.");
        section(pdf, "Getting started",
                "Acme Time Tracker runs in the browser; there is nothing to "
                        + "install. Your administrator sends you an "
                        + "invitation by email. Open the link in it, choose a "
                        + "password and you are ready to go.",
                "The start page shows this week. Each row is a project you "
                        + "work on, each column a day. Click a cell to record "
                        + "time.",
                "Use the menu on the left to move between your timesheets, "
                        + "your approvals and the reports.");
        section(pdf, "Signing in and passwords",
                "Sign in with your work email address and your password. If "
                        + "your organisation uses single sign-on, choose "
                        + "\"Sign in with your company account\" instead.",
                "To reset a forgotten password, choose \"Forgot password\" on "
                        + "the sign-in page and enter your email address. We "
                        + "send you a link that is valid for one hour. The "
                        + "new password must be at least 12 characters long.",
                "After five wrong passwords in a row, the account is locked "
                        + "for 15 minutes. Your administrator can unlock it "
                        + "sooner.");
        section(pdf, "Recording time",
                "Click a cell in the week view and type the hours, for "
                        + "example 1.5 or 1:30. Press Tab to move to the next "
                        + "day. A note is optional; add one when your project "
                        + "manager asks for details.",
                "To use the timer instead, click the play button next to a "
                        + "project. The timer keeps running when you close "
                        + "the browser, and stops at midnight at the latest.",
                "Time is rounded to the nearest five minutes when you submit "
                        + "the timesheet.");
        section(pdf, "Editing and deleting entries",
                "Click an entry to change its hours, project or note. To "
                        + "delete it, clear the hours and press Enter.",
                "You can edit entries until the timesheet is submitted. After "
                        + "that, ask your approver to send it back to you.");
        section(pdf, "Submitting a timesheet",
                "At the end of the week, check the totals and choose "
                        + "\"Submit\". The timesheet goes to your approver, "
                        + "and you get an email when it is approved or sent "
                        + "back.",
                "Days with less than the expected hours are marked in orange. "
                        + "You can still submit; add a note to explain the "
                        + "difference.");
        section(pdf, "Approving timesheets",
                "If you approve timesheets, the Approvals page lists the ones "
                        + "waiting for you. Open a timesheet to see the "
                        + "entries and notes.",
                "Choose \"Approve\" to accept it, or \"Send back\" with a "
                        + "comment to let the person fix it. You can approve "
                        + "several timesheets at once from the list.");
        section(pdf, "Reports",
                "The Reports page sums up time by project, person and week. "
                        + "Pick a period and the groups you want; the chart "
                        + "and the table update at once.",
                "Project managers see the projects they manage. "
                        + "Administrators see everything.");
        section(pdf, "Exporting data",
                "Any report can be exported as an Excel file or a CSV file. "
                        + "The export contains the rows of the table as "
                        + "shown, with the filters applied.",
                "For payroll, use the payroll export on the Administration "
                        + "page. It contains approved time only.");
        section(pdf, "Notifications",
                "We send an email reminder on Friday afternoon when your "
                        + "timesheet is not submitted. You can turn reminders "
                        + "off on your profile page.",
                "Approvers get a daily summary of timesheets waiting for "
                        + "them, instead of one email per timesheet.");
        section(pdf, "Keyboard shortcuts",
                "N starts a new entry, T starts or stops the timer, and the "
                        + "arrow keys move between cells. Press ? anywhere to "
                        + "see all shortcuts.");
        section(pdf, "Troubleshooting",
                "If the week view stays empty, check the date at the top: you "
                        + "may be looking at another week. If the timer does "
                        + "not start, another timer is probably running on a "
                        + "different project; stop it first.",
                "Still stuck? Choose \"Help\" in the menu to contact support. "
                        + "Include the time and what you were doing, so we "
                        + "can find the problem faster.");
        byte[] bytes = pdf.build();
        return new Manual(new Pdf("time-tracker-guide.pdf", bytes),
                pdf.headingPages());
    }

    /**
     * The service agreement of UC7, as an outside party would send it: the
     * application did not generate it and knows nothing about its layout.
     */
    public static Pdf serviceAgreement() {
        PdfBuilder pdf = new PdfBuilder("Master Service Agreement").bodySize(13)
                .pageSize(PDRectangle.A4, 80);
        pdf.paragraph("Between Acme Software Ltd (the \"Supplier\") and "
                + "Nordwind Logistics GmbH (the \"Customer\").");
        section(pdf, "1. Definitions",
                "\"Services\" means the hosted software and the support "
                        + "described in Schedule 1. \"Service Levels\" means "
                        + "the availability and response times in Schedule 2. "
                        + "\"Confidential Information\" means any information "
                        + "marked as confidential, or that a reasonable "
                        + "person would see as confidential.",
                "Headings are for convenience only. Words in the singular "
                        + "include the plural and the other way round.");
        section(pdf, "2. Services",
                "The Supplier provides the Services from the start date, with "
                        + "reasonable skill and care and in line with good "
                        + "industry practice.",
                "The Customer provides the information and access the "
                        + "Supplier reasonably needs, and makes sure its users "
                        + "follow the acceptable use policy.",
                "The Supplier may change the Services, as long as the change "
                        + "does not reduce their functionality in a material "
                        + "way.");
        section(pdf, "3. Service levels",
                "The Supplier meets the Service Levels in Schedule 2. Each "
                        + "month it reports the availability it achieved.",
                "If availability falls below 99.5% in a month, the Customer "
                        + "gets a service credit of 5% of the monthly fee for "
                        + "every full 0.5% below that level, up to 25% of the "
                        + "monthly fee.");
        section(pdf, "4. Fees and payment",
                "The Customer pays the fees in Schedule 3. The Supplier "
                        + "invoices the fees monthly in advance, and the "
                        + "Customer pays each invoice within 30 days of its "
                        + "date.",
                "Late payments carry interest at 8% above the base rate of "
                        + "the European Central Bank. If an invoice is more "
                        + "than 45 days overdue, the Supplier may suspend the "
                        + "Services after a written warning.",
                "The Supplier may raise the fees once a year, by no more than "
                        + "the consumer price index plus 3%.");
        section(pdf, "5. Term",
                "This agreement starts on the start date and runs for an "
                        + "initial term of 24 months. After that, it renews "
                        + "for further periods of 12 months each.",
                "Either party may stop a renewal by giving notice at least "
                        + "three months before the end of the current term.");
        section(pdf, "6. Termination",
                "Either party may end this agreement at once by written "
                        + "notice if the other party seriously breaks it and "
                        + "does not fix the breach within 30 days of being "
                        + "asked to, or if the other party becomes insolvent.",
                "When the agreement ends, the Customer stops using the "
                        + "Services and the Supplier makes the Customer's data "
                        + "available for export for 60 days, then deletes it.",
                "Clauses 7, 9 and 12 stay in force after the agreement ends.");
        section(pdf, "7. Confidentiality",
                "Each party keeps the other party's Confidential Information "
                        + "secret, uses it only for this agreement, and shares "
                        + "it only with employees and advisers who need to "
                        + "know it and are bound by the same duty.",
                "This duty does not cover information that is public, that "
                        + "the receiving party already had, or that the law "
                        + "requires it to disclose.");
        section(pdf, "8. Data protection",
                "The Supplier processes personal data only on the Customer's "
                        + "documented instructions, as set out in the data "
                        + "processing agreement in Schedule 4.",
                "The Supplier tells the Customer without undue delay, and in "
                        + "any case within 36 hours, after it becomes aware of "
                        + "a personal data breach.");
        section(pdf, "9. Limitation of liability",
                "Neither party is liable for loss of profits, revenue, "
                        + "goodwill or data, or for indirect or consequential "
                        + "loss.",
                "Each party's total liability in any contract year is limited "
                        + "to the fees paid or payable in that year. These "
                        + "limits do not apply to death or personal injury "
                        + "caused by negligence, to fraud, or to breaches of "
                        + "clause 7.");
        section(pdf, "10. Indemnity",
                "The Supplier defends the Customer against claims that the "
                        + "Services infringe the intellectual property rights "
                        + "of a third party, and pays the damages finally "
                        + "awarded.",
                "The Customer tells the Supplier about such a claim promptly "
                        + "and lets the Supplier control its defence.");
        section(pdf, "11. Force majeure",
                "Neither party is liable for a delay or failure caused by "
                        + "events beyond its reasonable control. If such an "
                        + "event lasts more than 60 days, either party may "
                        + "end this agreement by written notice.");
        section(pdf, "12. Governing law",
                "This agreement is governed by the laws of Ireland. The "
                        + "courts of Dublin have exclusive jurisdiction over "
                        + "any dispute arising from it.",
                "Before going to court, the parties try to settle a dispute "
                        + "through their senior managers within 30 days.");
        return new Pdf("master-service-agreement.pdf", pdf.build());
    }

    /** An expense claim with its receipt, for UC5. */
    public record ExpenseClaim(String id, String employee, String merchant,
            String date, BigDecimal claimed, List<String[]> receiptLines,
            BigDecimal receiptTotal) {

        public Pdf receipt() {
            PdfBuilder pdf = new PdfBuilder(merchant).bodySize(9)
                    .pageSize(new PDRectangle(80 * MM, 140 * MM), 6 * MM)
                    .withoutPageNumbers();
            pdf.paragraph(date);
            for (String[] line : receiptLines) {
                pdf.row(line[0], line[1]);
            }
            pdf.boldRow("TOTAL", money(receiptTotal));
            pdf.paragraph("Paid by card. Thank you for your visit!");
            return new Pdf("receipt-" + id + ".pdf", pdf.build());
        }
    }

    /** The expense claims of UC5. The first one claims more than it paid. */
    public static final List<ExpenseClaim> EXPENSE_CLAIMS = List.of(
            new ExpenseClaim("EXP-118", "Maria Lind", "Cafe Aurora",
                    "12.09.2026 12:41", new BigDecimal("32.40"),
                    List.of(new String[] { "2 x Lunch menu", "19.80 EUR" },
                            new String[] { "2 x Mineral water", "3.60 EUR" }),
                    new BigDecimal("23.40")),
            new ExpenseClaim("EXP-119", "Jonas Berg", "City Taxi",
                    "14.09.2026 07:55", new BigDecimal("41.00"),
                    List.of(new String[] { "Airport transfer", "38.00 EUR" },
                            new String[] { "Booking fee", "3.00 EUR" }),
                    new BigDecimal("41.00")),
            new ExpenseClaim("EXP-120", "Maria Lind", "Hotel Meridian",
                    "15.09.2026 09:10", new BigDecimal("129.00"),
                    List.of(new String[] { "1 night, single room",
                            "119.00 EUR" },
                            new String[] { "City tax", "10.00 EUR" }),
                    new BigDecimal("129.00")));

    /** An order waiting to be shipped, for UC6. */
    public record Order(String number, String recipient, List<String> address,
            String weight) {

        public Pdf label() {
            PdfBuilder pdf = new PdfBuilder("Acme Parcel")
                    .pageSize(new PDRectangle(4 * 72, 6 * 72), 18)
                    .withoutPageNumbers().bodySize(11);
            pdf.paragraph("From: Acme Software Ltd, 1 Harbour Lane, Dublin");
            pdf.heading("Ship to");
            pdf.large(recipient);
            address.forEach(pdf::paragraph);
            pdf.row("Order " + number, weight);
            pdf.bars();
            pdf.paragraph("AP " + number.replace("-", "") + " IE");
            return new Pdf("label-" + number + ".pdf", pdf.build());
        }
    }

    /** The orders of UC6. */
    public static final List<Order> ORDERS = List.of(
            new Order("A-20931", "Lumen Dental Oy",
                    List.of("Mannerheimintie 40", "00100 Helsinki", "Finland"),
                    "1.2 kg"),
            new Order("A-20932", "Casa Verde S.L.",
                    List.of("Calle Mayor 7", "28013 Madrid", "Spain"),
                    "0.4 kg"),
            new Order("A-20933", "Nordwind Logistics GmbH",
                    List.of("Hafenstrasse 12", "20457 Hamburg", "Germany"),
                    "3.8 kg"));

    /** The travel policy of the UC3 library. */
    public static Pdf travelPolicy() {
        PdfBuilder pdf = new PdfBuilder("Travel Policy");
        section(pdf, "Booking",
                "Book trips through the travel portal at least 14 days ahead. "
                        + "Economy class for flights under six hours.");
        section(pdf, "Hotels",
                "Up to 150 EUR a night in capital cities and 120 EUR "
                        + "elsewhere, breakfast included.");
        section(pdf, "Meals",
                "Meals are paid by the daily allowance. Do not claim meals "
                        + "separately, except for meals with customers.");
        section(pdf, "Claiming expenses",
                "Claim expenses within 30 days, with a receipt for every "
                        + "item.");
        return new Pdf("travel-policy.pdf", pdf.build());
    }

    /** The price list of the UC3 library. */
    public static Pdf priceList() {
        PdfBuilder pdf = new PdfBuilder("Price List 2026");
        pdf.boldRow("Product", "Per user and month");
        pdf.row("Acme Cloud Starter", money(new BigDecimal("6.00")));
        pdf.row("Acme Cloud Business", money(new BigDecimal("12.00")));
        pdf.row("Acme Cloud Enterprise", money(new BigDecimal("21.00")));
        pdf.row("Acme Time Tracker", money(new BigDecimal("4.50")));
        pdf.paragraph("Prices exclude VAT. Yearly billing gets two months "
                + "free.");
        return new Pdf("price-list-2026.pdf", pdf.build());
    }

    /**
     * A file that was cut off during an upload: it starts like a PDF, but most
     * of it is missing.
     */
    public static Pdf damagedReport() {
        byte[] complete = new PdfBuilder("Quarterly Report Q3")
                .paragraph("Revenue grew by 12% compared to Q2.").build();
        return new Pdf("quarterly-report-q3.pdf",
                Arrays.copyOf(complete, complete.length / 3));
    }

    /** A payslip that only opens with a password. */
    public static Pdf payslip() {
        PdfBuilder pdf = new PdfBuilder("Payslip September 2026")
                .password("lind-0926");
        pdf.row("Employee", "Maria Lind");
        pdf.row("Gross salary", money(new BigDecimal("4850.00")));
        pdf.row("Tax", money(new BigDecimal("-1211.40")));
        pdf.boldRow("Net salary", money(new BigDecimal("3638.60")));
        return new Pdf("payslip-2026-09.pdf", pdf.build());
    }

    private static void section(PdfBuilder pdf, String heading,
            String... paragraphs) {
        pdf.heading(heading);
        for (String paragraph : paragraphs) {
            pdf.paragraph(paragraph);
        }
    }
}
