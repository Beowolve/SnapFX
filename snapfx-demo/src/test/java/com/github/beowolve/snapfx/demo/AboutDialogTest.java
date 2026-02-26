package com.github.beowolve.snapfx.demo;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AboutDialogTest {
    @Test
    void testAboutDialogLogoResourcesExistInClasspath() {
        assertNotNull(
            AboutDialog.class.getResource(AboutDialog.getContentLogoResource()),
            "Missing about-dialog content logo resource"
        );
        assertNotNull(
            AboutDialog.class.getResource(AboutDialog.getDialogIconResource()),
            "Missing about-dialog window icon resource"
        );
    }

    @Test
    void testAboutDialogCreditUrlsMatchExpectedTargets() {
        URI flaticon = URI.create(AboutDialog.getFlaticonCreditUrl());
        assertEquals("https", flaticon.getScheme());
        assertEquals("www.flaticon.com", flaticon.getHost());
        assertEquals("/free-icons/logout", flaticon.getPath());

        URI yusukeAuthor = URI.create(AboutDialog.getYusukeAuthorUrl());
        assertEquals("p.yusukekamiyamane.com", yusukeAuthor.getHost());

        URI yusukeLicense = URI.create(AboutDialog.getYusukeLicenseUrl());
        assertEquals("creativecommons.org", yusukeLicense.getHost());
    }
}
