package com.example.uc6;

import com.example.ClipboardTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = CopyFromContextMenuView.class)
class CopyFromContextMenuViewTest extends SpringBrowserlessTest {

    @Test
    void theMenuItemOfTheTokensContextMenuCopiesIt() {
        CopyFromContextMenuView view = navigate(CopyFromContextMenuView.class);
        Pre token = findInView(Pre.class).single();
        assertEquals(token, view.menu.getTarget());
        test(view.menu).open();
        MenuItem copyItem = view.menu.getItems().stream()
                .filter(item -> "Copy value".equals(item.getText())).findFirst()
                .orElseThrow();

        ClipboardTestSupport.reportCopied(copyItem, token.getText());

        assertEquals("Token copied",
                test(find(Notification.class).single()).getText());
    }
}
