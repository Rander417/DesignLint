package designlint.fixtures;

import java.util.Optional;

/**
 * Test fixture: Optional misused as field type and method parameter.
 * Should trigger violations for both.
 */
public class OptionalAsFieldAndParam {
    private Optional<String> nickname = Optional.empty();

    public void setNickname(Optional<String> nickname) {
        this.nickname = nickname;
    }

    public Optional<String> getNickname() {
        return nickname;
    }
}
