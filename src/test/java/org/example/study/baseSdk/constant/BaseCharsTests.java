package org.example.study.baseSdk.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseCharsTests {

    @Test
    void shouldProvideCommonStringChars() {
        assertThat(BaseChars.EMPTY).isEmpty();
        assertThat(BaseChars.SPACE).isEqualTo(" ");
        assertThat(BaseChars.COMMA).isEqualTo(",");
        assertThat(BaseChars.DOT).isEqualTo(".");
        assertThat(BaseChars.COLON).isEqualTo(":");
        assertThat(BaseChars.SLASH).isEqualTo("/");
        assertThat(BaseChars.BACKSLASH).isEqualTo("\\");
        assertThat(BaseChars.UNDERLINE).isEqualTo("_");
        assertThat(BaseChars.HYPHEN).isEqualTo("-");
        assertThat(BaseChars.PIPE).isEqualTo("|");
    }

    @Test
    void shouldProvideCommonCharValues() {
        assertThat(BaseChars.CHAR_SPACE).isEqualTo(' ');
        assertThat(BaseChars.CHAR_COMMA).isEqualTo(',');
        assertThat(BaseChars.CHAR_DOT).isEqualTo('.');
        assertThat(BaseChars.CHAR_COLON).isEqualTo(':');
        assertThat(BaseChars.CHAR_UNDERLINE).isEqualTo('_');
        assertThat(BaseChars.CHAR_HYPHEN).isEqualTo('-');
    }

    @Test
    void shouldRejectInstantiation() throws NoSuchMethodException {
        Constructor<BaseChars> constructor = BaseChars.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .cause()
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("BaseChars cannot be instantiated");
    }
}
