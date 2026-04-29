package com.actacofrade.backend.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizationUtilsTest {

    @Test
    void sanitize_removesHtmlTags() {
        assertThat(SanitizationUtils.sanitize("<b>hola</b>")).isEqualTo("hola");
    }

    @Test
    void sanitize_collapsesMultipleSpaces() {
        assertThat(SanitizationUtils.sanitize("a    b  c")).isEqualTo("a b c");
    }

    @Test
    void sanitize_trimsResult() {
        assertThat(SanitizationUtils.sanitize("   hola   ")).isEqualTo("hola");
    }

    @Test
    void sanitize_nullReturnsNull() {
        assertThat(SanitizationUtils.sanitize(null)).isNull();
    }

    @Test
    void sanitizeNullable_blankReturnsNull() {
        assertThat(SanitizationUtils.sanitizeNullable("   ")).isNull();
        assertThat(SanitizationUtils.sanitizeNullable(null)).isNull();
        assertThat(SanitizationUtils.sanitizeNullable("")).isNull();
    }

    @Test
    void sanitizeNullable_returnsSanitized() {
        assertThat(SanitizationUtils.sanitizeNullable("<b>hi</b>")).isEqualTo("hi");
    }
}
