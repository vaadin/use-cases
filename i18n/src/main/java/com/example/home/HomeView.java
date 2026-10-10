package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.TranslatedTextView;
import com.example.uc10.TranslatedDataView;
import com.example.uc2.LanguageSwitcherView;
import com.example.uc3.DatesAndTimesView;
import com.example.uc4.NumbersAndCurrencyView;
import com.example.uc5.TimeZonesView;
import com.example.uc6.RightToLeftView;
import com.example.uc7.MixedDirectionView;
import com.example.uc8.LocaleAwareSortingView;
import com.example.uc9.TranslationSignalView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Internationalization Use Cases")
@UseCaseDescription("Making a Vaadin application work in the user's language, formats and writing direction")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Internationalization — use cases",
                "Each card below makes an application work for users in "
                        + "another language: translated texts and a language "
                        + "picker, dates, numbers, money and time zones in "
                        + "the user's conventions, right-to-left layouts for "
                        + "Arabic and Hebrew, and data that administrators "
                        + "translate themselves. Pick a language in the top "
                        + "bar; every use case follows it. API-GAPS.md "
                        + "records where Flow made that harder than it "
                        + "should be.");
        addMenuCards(
                List.of(TranslatedTextView.class, LanguageSwitcherView.class,
                        DatesAndTimesView.class, NumbersAndCurrencyView.class,
                        TimeZonesView.class, RightToLeftView.class,
                        MixedDirectionView.class, LocaleAwareSortingView.class,
                        TranslationSignalView.class, TranslatedDataView.class));
    }
}
